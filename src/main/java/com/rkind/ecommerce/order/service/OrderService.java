package com.rkind.ecommerce.order.service;

import com.rkind.ecommerce.order.dto.request.CreateOrderRequest;
import com.rkind.ecommerce.order.dto.request.OrderItemRequest;
import com.rkind.ecommerce.order.dto.response.OrderItemResponse;
import com.rkind.ecommerce.order.dto.response.OrderResponse;
import com.rkind.ecommerce.order.entity.*;
import com.rkind.ecommerce.order.repository.OrderItemRepository;
import com.rkind.ecommerce.order.repository.OrderRepository;
import com.rkind.ecommerce.product.entity.Product;
import com.rkind.ecommerce.product.entity.SalesChannel;
import com.rkind.ecommerce.product.repository.ProductRepository;
import com.rkind.ecommerce.shipping.service.ShiprocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.auth.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.rkind.ecommerce.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ShiprocketService shiprocketService;

    private static final Logger log =
            LoggerFactory.getLogger(OrderService.class);

    
    public OrderResponse createOrder(
            CreateOrderRequest request,
            Long userId
    ) {

        if (request == null) {
            throw new IllegalArgumentException("Order request is required.");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        if (userId == null) {
            throw new IllegalArgumentException("User is required.");
        }

        // ---------------------------------------------------------
        // 1. Validate fulfillment type
        // ---------------------------------------------------------

        if (request.getFulfillmentType() == null ||
                request.getFulfillmentType().isBlank()) {

            throw new IllegalArgumentException(
                    "Fulfillment type is required."
            );
        }

        SalesChannel salesChannel;

        try {
            salesChannel = SalesChannel.valueOf(
                    request.getFulfillmentType().trim().toUpperCase()
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid fulfillment type."
            );
        }

        // ---------------------------------------------------------
        // 2. Validate customer details
        // ---------------------------------------------------------

        if (request.getCustomerName() == null ||
                request.getCustomerName().isBlank()) {

            throw new IllegalArgumentException(
                    "Customer name is required."
            );
        }

        if (request.getCustomerEmail() == null ||
                request.getCustomerEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "Customer email is required."
            );
        }

        // ---------------------------------------------------------
        // 3. Validate ONLINE address
        // ---------------------------------------------------------

        if (salesChannel == SalesChannel.ONLINE) {

            if (request.getAddressLine1() == null ||
                    request.getAddressLine1().isBlank()) {

                throw new IllegalArgumentException(
                        "Delivery address is required for online orders."
                );
            }

            if (request.getCity() == null ||
                    request.getCity().isBlank()) {

                throw new IllegalArgumentException(
                        "City is required for online orders."
                );
            }

            if (request.getState() == null ||
                    request.getState().isBlank()) {

                throw new IllegalArgumentException(
                        "State is required for online orders."
                );
            }

            if (request.getPinCode() == null ||
                    request.getPinCode().isBlank()) {

                throw new IllegalArgumentException(
                        "PIN code is required for online orders."
                );
            }
        }

        // ---------------------------------------------------------
        // 4. Create Order
        // ---------------------------------------------------------

        Order order = new Order();

        order.setOrderNumber(generateOrderNumber());

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found.")
                );

        order.setUser(user);

        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setCustomerPhone(request.getCustomerPhone());

        order.setFulfillmentType(
                salesChannel == SalesChannel.ONLINE
                        ? FulfillmentType.ONLINE
                        : FulfillmentType.STORE_PICKUP
        );

        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        order.setPaymentMethod(request.getPaymentMethod());

        // ---------------------------------------------------------
        // 5. Address snapshot
        // ---------------------------------------------------------

        if (salesChannel == SalesChannel.ONLINE) {

            order.setAddressLine1(request.getAddressLine1());
            order.setAddressLine2(request.getAddressLine2());
            order.setCity(request.getCity());
            order.setState(request.getState());
            order.setPinCode(request.getPinCode());

        } else {

            order.setAddressLine1(null);
            order.setAddressLine2(null);
            order.setCity(null);
            order.setState(null);
            order.setPinCode(null);

            order.setPickupStore(request.getPickupStore());
        }

        // ---------------------------------------------------------
        // 6. Create order items
        // ---------------------------------------------------------

        List<OrderItem> orderItems = new ArrayList<>();

        BigDecimal productTotal = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {

            if (itemRequest.getProductId() == null) {
                throw new IllegalArgumentException(
                        "Product ID is required."
                );
            }

            if (itemRequest.getQuantity() == null ||
                    itemRequest.getQuantity() <= 0) {

                throw new IllegalArgumentException(
                        "Product quantity must be greater than zero."
                );
            }

            Product product = productRepository
                    .findById(itemRequest.getProductId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Product not found: "
                                            + itemRequest.getProductId()
                            )
                    );

            // -----------------------------------------------------
            // 7. Product must be active
            // -----------------------------------------------------

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException(
                        "Product is currently unavailable: "
                                + product.getName()
                );
            }

            // -----------------------------------------------------
            // 8. Product channel validation
            // -----------------------------------------------------

            if (product.getSalesChannel() != salesChannel) {

                throw new IllegalArgumentException(
                        "Product '" + product.getName()
                                + "' is not available for "
                                + salesChannel
                );
            }

            // -----------------------------------------------------
            // 9. Stock validation
            // -----------------------------------------------------

            if (product.getStock() == null ||
                    product.getStock() < itemRequest.getQuantity()) {

                throw new IllegalArgumentException(
                        "Insufficient stock for product: "
                                + product.getName()
                );
            }

            // -----------------------------------------------------
            // 10. Use actual DB price
            // -----------------------------------------------------

            BigDecimal unitPrice = product.getPrice();

            if (unitPrice == null ||
                    unitPrice.compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "Invalid product price for: "
                                + product.getName()
                );
            }

            BigDecimal itemTotal = unitPrice.multiply(
                    BigDecimal.valueOf(itemRequest.getQuantity())
            );

            OrderItem orderItem = new OrderItem();

            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setTotalPrice(itemTotal);

            orderItems.add(orderItem);

            productTotal = productTotal.add(itemTotal);
        }

        // ---------------------------------------------------------
        // 11. Discount
        // ---------------------------------------------------------

        BigDecimal discount = request.getDiscount();

        if (discount == null ||
                discount.compareTo(BigDecimal.ZERO) < 0) {

            discount = BigDecimal.ZERO;
        }

        if (discount.compareTo(productTotal) > 0) {
            throw new IllegalArgumentException(
                    "Discount cannot be greater than product total."
            );
        }

        // ---------------------------------------------------------
        // 12. Delivery charge
        // ---------------------------------------------------------

        /*
         * STORE_PICKUP:
         *     Delivery = 0
         *
         * ONLINE:
         *     Shiprocket integration will calculate the actual charge.
         *
         * For now we keep it zero until Shiprocket quote is connected.
         */

        BigDecimal deliveryCharge = BigDecimal.ZERO;

// Delivery charge for online orders
        if (salesChannel == SalesChannel.ONLINE) {

            deliveryCharge = request.getDeliveryCharge() == null
                    ? BigDecimal.ZERO
                    : request.getDeliveryCharge();

            if (deliveryCharge.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Delivery charge cannot be negative."
                );
            }
        }

// Final total
        BigDecimal totalAmount = productTotal
                .subtract(discount)
                .add(deliveryCharge);

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Order total cannot be negative."
            );
        }

        order.setProductTotal(productTotal);
        order.setDeliveryCharge(deliveryCharge);
        order.setDiscount(discount);
        order.setTotalAmount(totalAmount);

        // ---------------------------------------------------------
        // 14. Save order
        // ---------------------------------------------------------

        Order savedOrder = orderRepository.save(order);

        // ---------------------------------------------------------
        // 15. Attach order to every item and save
        // ---------------------------------------------------------

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrder(savedOrder);
        }

        orderItemRepository.saveAll(orderItems);

        // ---------------------------------------------------------
        // 16. Return response
        // ---------------------------------------------------------

        return mapToResponse(savedOrder, orderItems);
    }



    public OrderResponse createPaidOrder(
            CreateOrderRequest request,
            Long userId,
            String razorpayOrderId,
            String razorpayPaymentId,
            int amountInPaise
    ) {
        // Avoid duplicate orders when the payment callback is retried.
        var existing = orderRepository.findByRazorpayOrderId(razorpayOrderId);

        if (existing.isPresent()) {
            Order order = existing.get();

            if (order.getUser().getId().equals(userId)
                    && razorpayPaymentId.equals(order.getRazorpayPaymentId())
                    && order.getPaymentStatus() == PaymentStatus.PAID) {

                List<OrderItem> items =
                        orderItemRepository.findByOrderId(order.getId());

                return mapToResponse(order, items);
            }

            throw new IllegalArgumentException(
                    "This Razorpay order has already been processed."
            );
        }

        // Existing createOrder calculates product total from database prices.
        OrderResponse response = createOrder(request, userId);

        BigDecimal paidAmount = BigDecimal.valueOf(amountInPaise)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);

        // The captured amount must match the calculated application order.
        if (response.getTotalAmount().compareTo(paidAmount) != 0) {
            throw new IllegalArgumentException(
                    "Payment amount does not match the calculated order total."
            );
        }

        Order order = orderRepository.findById(response.getId())
                .orElseThrow(() ->
                        new IllegalStateException("Created order could not be found.")
                );

        order.setRazorpayOrderId(razorpayOrderId);
        order.setRazorpayPaymentId(razorpayPaymentId);
        order.setPaymentMethod("RAZORPAY");
        order.setPaymentStatus(PaymentStatus.PAID);

        orderRepository.save(order);

        response.setRazorpayOrderId(razorpayOrderId);
        response.setRazorpayPaymentId(razorpayPaymentId);
        response.setPaymentMethod("RAZORPAY");
        response.setPaymentStatus(PaymentStatus.PAID.name());

        return response;
    }


    // =============================================================
    // Generate unique order number
    // =============================================================

    private String generateOrderNumber() {

        String orderNumber;

        do {
            String randomPart = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();

            orderNumber = "RK-" + randomPart;

        } while (orderRepository
                .findByOrderNumber(orderNumber)
                .isPresent());

        return orderNumber;
    }

    // =============================================================
    // Map Entity -> Response
    // =============================================================

    private OrderResponse mapToResponse(
            Order order,
            List<OrderItem> orderItems
    ) {

        List<OrderItemResponse> itemResponses =
                new ArrayList<>();

        for (OrderItem item : orderItems) {

            String productImage = null;

            if (item.getProduct() != null &&
                    item.getProduct().getImageUrls() != null &&
                    !item.getProduct().getImageUrls().isEmpty()) {

                productImage =
                        item.getProduct().getImageUrls().get(0);
            }

            itemResponses.add(
                    OrderItemResponse.builder()
                            .productId(
                                    item.getProduct().getId()
                            )
                            .productName(
                                    item.getProductName()
                            )
                            .productImage(productImage)
                            .unitPrice(
                                    item.getUnitPrice()
                            )
                            .quantity(
                                    item.getQuantity()
                            )
                            .orderItemId(item.getId())
                            .totalPrice(
                                    item.getTotalPrice()
                            )
                            .build()
            );
        }

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .fulfillmentType(
                        order.getFulfillmentType().name()
                )
                .orderStatus(
                        order.getOrderStatus().name()
                )
                .paymentStatus(
                        order.getPaymentStatus().name()
                )
                .paymentMethod(order.getPaymentMethod())
                .razorpayOrderId(order.getRazorpayOrderId())
                .razorpayPaymentId(order.getRazorpayPaymentId())
                .productTotal(order.getProductTotal())
                .deliveryCharge(order.getDeliveryCharge())
                .discount(order.getDiscount())
                .totalAmount(order.getTotalAmount())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .addressLine1(order.getAddressLine1())
                .addressLine2(order.getAddressLine2())
                .city(order.getCity())
                .state(order.getState())
                .pinCode(order.getPinCode())
                .pickupStore(order.getPickupStore())
                .expectedPickupTime(
                        order.getExpectedPickupTime()
                )
                .pickedUpAt(
                        order.getPickedUpAt()
                )
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .shiprocketStatus(order.getShiprocketStatus())
                .shiprocketShipmentId(order.getShiprocketShipmentId())
                .shiprocketAwbCode(order.getShiprocketAwbCode())
                .shiprocketError(order.getShiprocketError())
                .build();
    }



    public List<OrderResponse> getMyOrders(Long userId) {

        List<Order> orders =
                orderRepository.findByUser_IdOrderByCreatedAtDesc(userId);

        List<OrderResponse> responses = new ArrayList<>();

        for (Order order : orders) {

            List<OrderItem> items =
                    orderItemRepository.findByOrderId(order.getId());

            responses.add(mapToResponse(order, items));
        }

        return responses;
    }



    public OrderResponse acceptOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found.")
                );

        // If already confirmed, retry Shiprocket without deducting stock again.
        if (order.getOrderStatus() == OrderStatus.CONFIRMED) {

            List<OrderItem> items =
                    orderItemRepository.findByOrderId(order.getId());

            if (order.getFulfillmentType() == FulfillmentType.ONLINE
                    && !"COMPLETED".equals(order.getShiprocketStatus())) {

                try {
                    shiprocketService.createAndRequestPickup(order, items);

                } catch (Exception exception) {

                    if (order.getShiprocketStatus() == null
                            || order.getShiprocketStatus().isBlank()) {
                        order.setShiprocketStatus("FAILED");
                    }

                    order.setShiprocketError(exception.getMessage());

                    log.error(
                            "Shiprocket retry failed for order {}",
                            order.getOrderNumber(),
                            exception
                    );
                }

                orderRepository.save(order);
            }

            return mapToResponse(order, items);
        }

        // Only pending orders can be accepted.
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending orders can be accepted."
            );
        }

        List<OrderItem> items =
                orderItemRepository.findByOrderId(order.getId());

        // Validate and deduct stock for every ordered product.
        for (OrderItem item : items) {

            if (item.getProduct() == null) {
                throw new IllegalStateException(
                        "Product not found for order item."
                );
            }

            Product product = productRepository
                    .findById(item.getProduct().getId())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Product not found for order item."
                            )
                    );

            Integer currentStock = product.getStock();
            Integer orderedQuantity = item.getQuantity();

            if (currentStock == null
                    || orderedQuantity == null
                    || orderedQuantity <= 0
                    || currentStock < orderedQuantity) {

                throw new IllegalStateException(
                        "Insufficient stock for product: "
                                + product.getName()
                );
            }

            product.setStock(currentStock - orderedQuantity);
            productRepository.save(product);
        }

        // Confirm the order AFTER stock validation and deduction.
        order.setOrderStatus(OrderStatus.CONFIRMED);

        // Save the confirmed order. Declare savedOrder only once.
        Order savedOrder = orderRepository.save(order);

        // Shiprocket is only for online delivery orders.
        // Store-pickup orders skip Shiprocket.
        if (order.getFulfillmentType() == FulfillmentType.ONLINE) {

            try {
                shiprocketService.createAndRequestPickup(order, items);

            } catch (Exception exception) {

                if (order.getShiprocketStatus() == null
                        || order.getShiprocketStatus().isBlank()) {
                    order.setShiprocketStatus("FAILED");
                }

                order.setShiprocketError(exception.getMessage());

                log.error(
                        "Shiprocket failed for order {}",
                        order.getOrderNumber(),
                        exception
                );
            }

            // Persist Shiprocket status, IDs, and any error.
            savedOrder = orderRepository.save(order);
        }

        // Send confirmation email to customer.
        try {

            String subject = "Your Order Has Been Accepted - "
                    + savedOrder.getOrderNumber();

            String body =
                    "Hi " + savedOrder.getCustomerName() + ",\n\n"
                            + "Good news! Your order has been accepted.\n\n"
                            + "Order Number: " + savedOrder.getOrderNumber() + "\n"
                            + "Order Status: CONFIRMED\n"
                            + "Total Amount: Rs. " + savedOrder.getTotalAmount() + "\n\n"
                            + "We will keep you updated about your order.\n\n"
                            + "Google Maps Location:\n"
                            + "https://maps.app.goo.gl/M2mmCK3ibD3hAVee9\n\n"
                            + "Thank you for shopping with us!\n"
                            + "Rajesh Stationery & Novelty";

            emailService.sendEmail(
                    savedOrder.getCustomerEmail(),
                    subject,
                    body
            );

        } catch (Exception exception) {

            log.error(
                    "Order {} was confirmed, but the email could not be sent.",
                    savedOrder.getOrderNumber(),
                    exception
            );
        }

        return mapToResponse(savedOrder, items);
    }


    // =============================================================
    // MARK OFFLINE STORE ORDER AS PACKED
    // =============================================================
    public OrderResponse markOrderPacked(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found.")
                );

        // This workflow is exclusively for offline store pickup.
        if (order.getFulfillmentType() != FulfillmentType.STORE_PICKUP) {
            throw new IllegalStateException(
                    "Packing workflow is only for offline store-pickup orders."
            );
        }

        if (order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only accepted orders can be marked as packed."
            );
        }

        order.setOrderStatus(OrderStatus.PACKED);
        Order savedOrder = orderRepository.save(order);

        sendStorePickupStatusEmail(
                savedOrder,
                "Your Order Is Packed",
                "Your order has been packed and is ready for store pickup.",
                "PACKED"
        );

        List<OrderItem> items =
                orderItemRepository.findByOrderId(savedOrder.getId());

        return mapToResponse(savedOrder, items);
    }


    // =============================================================
    // MARK OFFLINE STORE ORDER AS PICKED UP
    // =============================================================
    public OrderResponse markOrderPickedUp(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found.")
                );

        // Never run this workflow for online Shiprocket orders.
        if (order.getFulfillmentType() != FulfillmentType.STORE_PICKUP) {
            throw new IllegalStateException(
                    "Pickup workflow is only for offline store-pickup orders."
            );
        }

        if (order.getOrderStatus() != OrderStatus.PACKED) {
            throw new IllegalStateException(
                    "Only packed orders can be marked as picked up."
            );
        }

        LocalDateTime pickupTime = LocalDateTime.now();

        order.setOrderStatus(OrderStatus.PICKED_UP);
        order.setPickedUpAt(pickupTime);

        Order savedOrder = orderRepository.save(order);

        sendStorePickupStatusEmail(
                savedOrder,
                "Your Order Has Been Picked Up",
                "Your order has been picked up successfully.\n"
                        + "Pickup time: " + pickupTime
                        + "\n\nThank you for shopping with us!",
                "PICKED_UP"
        );

        List<OrderItem> items =
                orderItemRepository.findByOrderId(savedOrder.getId());

        return mapToResponse(savedOrder, items);
    }


    // =============================================================
    // EMAIL NOTIFICATION FOR OFFLINE STORE-PICKUP STATUS
    // =============================================================
    private void sendStorePickupStatusEmail(
            Order order,
            String emailTitle,
            String message,
            String status
    ) {

        try {
            String subject = emailTitle + " - " + order.getOrderNumber();

            String body =
                    "Hi " + order.getCustomerName() + ",\n\n"
                            + message + "\n\n"
                            + "Order Number: " + order.getOrderNumber() + "\n"
                            + "Order Status: " + status + "\n"
                            + "Total Amount: Rs. " + order.getTotalAmount()
                            + "\n\n"
                            + "Rajesh Stationery & Novelty";

            emailService.sendEmail(
                    order.getCustomerEmail(),
                    subject,
                    body
            );

        } catch (Exception exception) {
            log.error(
                    "Order status was updated to {}, but email failed for order {}",
                    status,
                    order.getOrderNumber(),
                    exception
            );
        }
    }




    public List<OrderResponse> getAllOrders() {

        List<Order> allOrders =
                orderRepository.findAll(
                        org.springframework.data.domain.Sort
                                .by(org.springframework.data.domain.Sort.Direction.DESC,
                                        "createdAt")
                );

        List<OrderResponse> responses = new ArrayList<>();

        for (Order order : allOrders) {
            List<OrderItem> items =
                    orderItemRepository.findByOrderId(order.getId());

            responses.add(mapToResponse(order, items));
        }

        return responses;
    }


    public java.util.Map<String, Object> getMyOrderTracking(
            Long orderId,
            Long userId
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found.")
                );

        // Customer can only track their own order.
        if (order.getUser() == null
                || !order.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not allowed to track this order."
            );
        }

        if (order.getFulfillmentType() != FulfillmentType.ONLINE) {
            throw new IllegalStateException(
                    "Live shipment tracking is only available for online delivery orders."
            );
        }

        String awbCode = order.getShiprocketAwbCode();

        if (awbCode == null || awbCode.isBlank()) {
            throw new IllegalStateException(
                    "Shipment AWB is not available yet. Please try again later."
            );
        }

        java.util.Map<String, Object> result =
                new java.util.LinkedHashMap<>();

        result.put("orderId", order.getId());
        result.put("orderNumber", order.getOrderNumber());
        result.put("awbCode", awbCode);
        result.put("shiprocketShipmentId", order.getShiprocketShipmentId());
        result.put("shiprocketStatus", order.getShiprocketStatus());

        // Fetch current tracking information from Shiprocket.
        result.put(
                "trackingDetails",
                shiprocketService.trackShipmentByAwb(awbCode)
        );

        return result;
    }


    public OrderResponse cancelOrder(Long orderId, Long userId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found."));

        // Customer can cancel only their own order.
        if (order.getUser() == null
                || !order.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not allowed to cancel this order.");
        }

        OrderStatus status = order.getOrderStatus();

        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled.");
        }

        if (status == OrderStatus.DELIVERED
                || status == OrderStatus.PICKED_UP) {
            throw new IllegalStateException(
                    "Completed orders cannot be cancelled.");
        }

        boolean online =
                order.getFulfillmentType() == FulfillmentType.ONLINE;

        if (online) {

            // Online orders can be cancelled only before dispatch.
            if (hasDispatchStarted(status.name())
                    || hasDispatchStarted(order.getShiprocketStatus())) {
                throw new IllegalStateException(
                        "This order can no longer be cancelled because dispatch has started.");
            }

        } else if (order.getFulfillmentType() == FulfillmentType.STORE_PICKUP) {

            LocalDateTime oneHourAfterOrder =
                    order.getCreatedAt().plusHours(1);

            boolean withinFirstHour =
                    !LocalDateTime.now().isAfter(oneHourAfterOrder);

            boolean packed = "PACKED".equals(status.name());

            // First hour: cancellation is allowed even if packed.
            // After first hour: cancellation is allowed only if not packed.
            if (!withinFirstHour && packed) {
                throw new IllegalStateException(
                        "This order is packed and the first hour has passed.");
            }

            if (!status.name().equals("PENDING")
                    && !status.name().equals("CONFIRMED")
                    && !packed) {
                throw new IllegalStateException(
                        "This store-pickup order cannot be cancelled in its current status.");
            }

        } else {
            throw new IllegalStateException(
                    "Unsupported order fulfillment type.");
        }

        // Load order items.
        List<OrderItem> items =
                orderItemRepository.findByOrderId(order.getId());

        // Stock is deducted when an order is accepted.
        // Restore it only when the order had progressed beyond PENDING.
        if (status != OrderStatus.PENDING) {
            for (OrderItem item : items) {

                if (item.getProduct() == null) {
                    throw new IllegalStateException(
                            "Product missing for an order item.");
                }

                Product product = productRepository
                        .findById(item.getProduct().getId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Product not found while restoring stock."));

                int currentStock = product.getStock() == null
                        ? 0 : product.getStock();

                int quantity = item.getQuantity() == null
                        ? 0 : item.getQuantity();

                product.setStock(currentStock + quantity);
                productRepository.save(product);
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        Order savedOrder = orderRepository.save(order);

        // Notify the customer; email failure must not undo cancellation.
        try {
            String subject =
                    "Order Cancelled - " + savedOrder.getOrderNumber();

            String body =
                    "Hi " + savedOrder.getCustomerName() + ",\n\n"
                            + "Your order has been cancelled successfully.\n\n"
                            + "Order Number: " + savedOrder.getOrderNumber() + "\n"
                            + "Order Status: CANCELLED\n\n"
                            + "If you paid online, any applicable refund "
                            + "will be handled separately.\n\n"
                            + "Rajesh Stationery & Novelty";

            emailService.sendEmail(
                    savedOrder.getCustomerEmail(),
                    subject,
                    body
            );

        } catch (Exception exception) {
            log.error(
                    "Order {} cancelled, but customer email failed.",
                    savedOrder.getOrderNumber(),
                    exception
            );
        }

        return mapToResponse(savedOrder, items);
    }

    private boolean hasDispatchStarted(String value) {

        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.trim()
                .toUpperCase()
                .replaceAll("[\\s-]+", "_");

        return normalized.contains("DISPATCH")
                || normalized.contains("SHIPPED")
                || normalized.contains("IN_TRANSIT")
                || normalized.contains("OUT_FOR_DELIVERY")
                || normalized.contains("DELIVERED")
                || normalized.contains("PICKED_UP_BY_COURIER");
    }

}