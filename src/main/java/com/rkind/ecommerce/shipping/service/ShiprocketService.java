package com.rkind.ecommerce.shipping.service;

import com.rkind.ecommerce.order.entity.Order;
import com.rkind.ecommerce.order.entity.OrderItem;
import com.rkind.ecommerce.shipping.config.ShiprocketConfig;
import com.rkind.ecommerce.shipping.dto.response.ShiprocketAuthResponse;
import com.rkind.ecommerce.shipping.dto.response.ShiprocketRateResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.rkind.ecommerce.order.entity.Order;
import com.rkind.ecommerce.order.entity.OrderItem;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;
import com.rkind.ecommerce.order.entity.Order;
import com.rkind.ecommerce.order.entity.OrderItem;
import com.rkind.ecommerce.product.entity.Product;
import tools.jackson.databind.JsonNode;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
@Service
public class ShiprocketService {

    private final ShiprocketConfig shiprocketConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ShiprocketService(ShiprocketConfig shiprocketConfig) {
        this.shiprocketConfig = shiprocketConfig;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // =========================================================
    // 1. SHIPROCKET AUTHENTICATION
    // =========================================================

    public String getAuthToken() {

        String url = shiprocketConfig.getBaseUrl() + "/auth/login";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", shiprocketConfig.getEmail());
        requestBody.put("password", shiprocketConfig.getPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, String>> requestEntity =
                new HttpEntity<>(requestBody, headers);

        try {

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException(
                        "Shiprocket authentication failed. Status: "
                                + response.getStatusCode()
                );
            }

            if (response.getBody() == null || response.getBody().isBlank()) {
                throw new IllegalStateException(
                        "Shiprocket authentication returned empty response."
                );
            }

            ShiprocketAuthResponse authResponse =
                    objectMapper.readValue(
                            response.getBody(),
                            ShiprocketAuthResponse.class
                    );

            if (authResponse.getToken() == null
                    || authResponse.getToken().isBlank()) {

                throw new IllegalStateException(
                        "Shiprocket authentication succeeded but token was not returned."
                );
            }

            return authResponse.getToken();

        } catch (HttpStatusCodeException e) {

            throw new IllegalStateException(
                    "Shiprocket authentication failed: "
                            + e.getResponseBodyAsString(),
                    e
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to authenticate with Shiprocket: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // 2. GET SHIPPING RATES
    // =========================================================

    public ShiprocketRateResponse.CourierCompany getShippingRate(
            String deliveryPincode,
            BigDecimal weight,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            boolean cod,
            BigDecimal declaredValue
    ) {

        validateShippingInputs(
                deliveryPincode,
                weight,
                length,
                width,
                height
        );

        String token = getAuthToken();

        URI uri = buildServiceabilityUri(
                deliveryPincode,
                weight,
                length,
                width,
                height,
                cod,
                declaredValue
        );

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);

        HttpEntity<Void> requestEntity =
                new HttpEntity<>(headers);

        try {

            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {

                throw new IllegalStateException(
                        "Shiprocket serviceability failed. Status: "
                                + response.getStatusCode()
                );
            }

            if (response.getBody() == null
                    || response.getBody().isBlank()) {

                throw new IllegalStateException(
                        "Shiprocket serviceability returned empty response."
                );
            }

            ShiprocketRateResponse rateResponse =
                    objectMapper.readValue(
                            response.getBody(),
                            ShiprocketRateResponse.class
                    );

            if (rateResponse.getData() == null
                    || rateResponse.getData()
                    .getAvailable_courier_companies() == null
                    || rateResponse.getData()
                    .getAvailable_courier_companies()
                    .isEmpty()) {

                throw new IllegalStateException(
                        "No courier is available for this delivery location."
                );
            }

            /*
             * Select the cheapest available courier.
             *
             * Shiprocket returns multiple courier options.
             * We use the lowest available rate as the actual
             * shipping quote for our checkout calculation.
             */
            return rateResponse.getData()
                    .getAvailable_courier_companies()
                    .stream()
                    .filter(courier ->
                            courier.getRate() != null
                                    && courier.getRate()
                                    .compareTo(BigDecimal.ZERO) >= 0
                    )
                    .min((c1, c2) ->
                            c1.getRate().compareTo(c2.getRate())
                    )
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Shiprocket returned couriers but no valid shipping rate was found."
                            )
                    );

        } catch (HttpStatusCodeException e) {

            throw new IllegalStateException(
                    "Shiprocket serviceability failed: "
                            + e.getResponseBodyAsString(),
                    e
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to get shipping rate from Shiprocket: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // 3. BUILD SERVICEABILITY URL
    // =========================================================

    private URI buildServiceabilityUri(
            String deliveryPincode,
            BigDecimal weight,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height,
            boolean cod,
            BigDecimal declaredValue
    ) {

        String pickupPincode =
                shiprocketConfig.getPickupPincode();

        UriComponentsBuilder builder =
                UriComponentsBuilder
                        .fromUriString(
                                shiprocketConfig.getBaseUrl()
                                        + "/courier/serviceability/"
                        )
                        .queryParam(
                                "pickup_postcode",
                                pickupPincode
                        )
                        .queryParam(
                                "delivery_postcode",
                                deliveryPincode
                        )
                        .queryParam(
                                "weight",
                                weight.stripTrailingZeros()
                                        .toPlainString()
                        )

                        .queryParam(
                                "cod",
                                cod ? 1 : 0
                        )

                        .queryParam(
                                "mode",
                                "Surface"
                        )

                        .queryParam(
                                "cod",
                                cod ? 1 : 0
                        );

        /*
         * Shiprocket supports dimensions for more precise
         * shipment-rate calculation.
         */
        if (length != null) {
            builder.queryParam(
                    "length",
                    dimensionValue(length)
            );
        }

        if (width != null) {
            builder.queryParam(
                    "breadth",
                    dimensionValue(width)
            );
        }

        if (height != null) {
            builder.queryParam(
                    "height",
                    dimensionValue(height)
            );
        }

        if (declaredValue != null
                && declaredValue.compareTo(BigDecimal.ZERO) > 0) {

            builder.queryParam(
                    "declared_value",
                    declaredValue
                            .setScale(0, RoundingMode.HALF_UP)
                            .toPlainString()
            );
        }

        return builder.build().encode().toUri();
    }

    // =========================================================
    // 4. VALIDATION
    // =========================================================

    private void validateShippingInputs(
            String deliveryPincode,
            BigDecimal weight,
            BigDecimal length,
            BigDecimal width,
            BigDecimal height
    ) {

        if (deliveryPincode == null
                || !deliveryPincode.matches("\\d{6}")) {

            throw new IllegalArgumentException(
                    "Valid 6-digit delivery pincode is required."
            );
        }

        if (shiprocketConfig.getPickupPincode() == null
                || !shiprocketConfig.getPickupPincode()
                .matches("\\d{6}")) {

            throw new IllegalStateException(
                    "Shiprocket pickup pincode is not configured correctly."
            );
        }

        if (weight == null
                || weight.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Shipment weight must be greater than zero."
            );
        }

        if (length != null
                && length.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Shipment length must be greater than zero."
            );
        }

        if (width != null
                && width.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Shipment width must be greater than zero."
            );
        }

        if (height != null
                && height.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Shipment height must be greater than zero."
            );
        }
    }

    // =========================================================
    // 5. DIMENSION FORMAT
    // =========================================================

    private String dimensionValue(BigDecimal value) {

        return value
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }


    public void createAndRequestPickup(
            Order order,
            List<OrderItem> items
    ) {
        if (order == null || items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Order and order items are required."
            );
        }

        // Do not process store-pickup orders.
        if (order.getFulfillmentType()
                != com.rkind.ecommerce.order.entity.FulfillmentType.ONLINE) {
            return;
        }

        // If pickup is already completed, do not repeat the API calls.
        if ("COMPLETED".equals(order.getShiprocketStatus())) {
            return;
        }

        String token = getAuthToken();

        try {
            Map<String, Object> createPayload = new HashMap<>();

            BigDecimal totalWeight = BigDecimal.ZERO;
            BigDecimal maxLength = BigDecimal.ZERO;
            BigDecimal maxWidth = BigDecimal.ZERO;
            BigDecimal maxHeight = BigDecimal.ZERO;

            List<Map<String, Object>> orderItems = new ArrayList<>();

            for (OrderItem item : items) {
                if (item.getProduct() == null) {
                    throw new IllegalStateException(
                            "Product is missing for an order item."
                    );
                }

                var product = item.getProduct();

                int quantity = item.getQuantity() == null
                        ? 0 : item.getQuantity();

                if (quantity <= 0) {
                    throw new IllegalStateException(
                            "Invalid quantity for " + product.getName()
                    );
                }

                if (product.getWeight() == null
                        || product.getWeight().compareTo(BigDecimal.ZERO) <= 0
                        || product.getLength() == null
                        || product.getWidth() == null
                        || product.getHeight() == null) {
                    throw new IllegalStateException(
                            "Valid weight and dimensions are required for product: "
                                    + product.getName()
                    );
                }

                totalWeight = totalWeight.add(
                        product.getWeight().multiply(
                                BigDecimal.valueOf(quantity)
                        )
                );

                maxLength = maxLength.max(product.getLength());
                maxWidth = maxWidth.max(product.getWidth());
                maxHeight = maxHeight.max(product.getHeight());

                Map<String, Object> productItem = new HashMap<>();
                productItem.put("name", product.getName());
                productItem.put(
                        "sku",
                        product.getSku() == null
                                ? "PRODUCT-" + product.getId()
                                : product.getSku()
                );
                productItem.put("units", quantity);
                productItem.put(
                        "selling_price",
                        product.getPrice() == null
                                ? "0.00"
                                : product.getPrice().toPlainString()
                );

                orderItems.add(productItem);
            }

            if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException(
                        "Total shipment weight must be greater than zero."
                );
            }

            if (order.getPinCode() == null
                    || !order.getPinCode().matches("\\d{6}")) {
                throw new IllegalStateException(
                        "A valid customer delivery pincode is required."
                );
            }

            String pickupLocation = shiprocketConfig.getPickupLocation();

            if (pickupLocation == null || pickupLocation.isBlank()) {
                throw new IllegalStateException(
                        "Configure the registered Shiprocket pickup location name."
                );
            }

            createPayload.put("order_id", order.getOrderNumber());
            createPayload.put(
                    "order_date",
                    order.getCreatedAt().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    )
            );
            createPayload.put("pickup_location", pickupLocation);

            createPayload.put(
                    "billing_customer_name",
                    order.getCustomerName()
            );
            createPayload.put("billing_last_name", "");
            createPayload.put(
                    "billing_address",
                    order.getAddressLine1()
            );
            createPayload.put(
                    "billing_address_2",
                    order.getAddressLine2() == null
                            ? "" : order.getAddressLine2()
            );
            createPayload.put("billing_city", order.getCity());
            createPayload.put("billing_state", order.getState());
            createPayload.put("billing_pincode", order.getPinCode());
            createPayload.put("billing_country", "India");
            createPayload.put("billing_email", order.getCustomerEmail());
            createPayload.put("billing_phone", order.getCustomerPhone());

            createPayload.put("shipping_is_billing", true);

            createPayload.put("order_items", orderItems);
            createPayload.put(
                    "payment_method",
                    "COD".equalsIgnoreCase(order.getPaymentMethod())
                            ? "COD" : "Prepaid"
            );

            createPayload.put(
                    "sub_total",
                    order.getProductTotal().toPlainString()
            );
            createPayload.put(
                    "shipping_charges",
                    order.getDeliveryCharge().toPlainString()
            );
            createPayload.put(
                    "total_discount",
                    order.getDiscount().toPlainString()
            );

            createPayload.put(
                    "length",
                    maxLength.setScale(0, RoundingMode.CEILING)
            );
            createPayload.put(
                    "breadth",
                    maxWidth.setScale(0, RoundingMode.CEILING)
            );
            createPayload.put(
                    "height",
                    maxHeight.setScale(0, RoundingMode.CEILING)
            );
            createPayload.put(
                    "weight",
                    totalWeight.setScale(3, RoundingMode.CEILING)
            );

            // Step 1: Create Shiprocket order only if shipment ID is absent.
            String shipmentId = order.getShiprocketShipmentId();

            if (shipmentId == null || shipmentId.isBlank()) {
                Map<String, Object> createResponse = postShiprocket(
                        "/orders/create/adhoc",
                        createPayload,
                        token
                );

                shipmentId = findResponseValue(
                        createResponse,
                        "shipment_id"
                );

                if (shipmentId == null || shipmentId.isBlank()) {
                    throw new IllegalStateException(
                            "Shiprocket did not return a shipment ID: "
                                    + createResponse
                    );
                }

                order.setShiprocketShipmentId(shipmentId);

                String shiprocketOrderId = findResponseValue(
                        createResponse,
                        "order_id"
                );

                if (shiprocketOrderId != null) {
                    order.setShiprocketOrderId(shiprocketOrderId);
                }

                order.setShiprocketStatus("SHIPMENT_CREATED");
                order.setShiprocketError(null);
            }

            // Step 2: Assign AWB only if it is not already present.
            String awb = order.getShiprocketAwbCode();

            if (awb == null || awb.isBlank()) {
                Map<String, Object> awbPayload = new HashMap<>();
                awbPayload.put("shipment_id", Long.valueOf(shipmentId));

                Map<String, Object> awbResponse = postShiprocket(
                        "/courier/assign/awb",
                        awbPayload,
                        token
                );

                awb = findResponseValue(awbResponse, "awb_code");

                if (awb == null || awb.isBlank()) {
                    throw new IllegalStateException(
                            "Shiprocket did not return an AWB: " + awbResponse
                    );
                }

                order.setShiprocketAwbCode(awb);
                order.setShiprocketStatus("AWB_ASSIGNED");
            }

            // Step 3: Request pickup.
            Map<String, Object> pickupPayload = new HashMap<>();
            pickupPayload.put("shipment_id", Long.valueOf(shipmentId));

            postShiprocket(
                    "/courier/generate/pickup",
                    pickupPayload,
                    token
            );

            order.setShiprocketStatus("PICKUP_REQUESTED");

            // Step 4: Generate manifest after AWB and pickup request.
            Map<String, Object> manifestPayload = new HashMap<>();
            manifestPayload.put(
                    "shipment_id",
                    List.of(Long.valueOf(shipmentId))
            );

            postShiprocket(
                    "/manifests/generate",
                    manifestPayload,
                    token
            );

            order.setShiprocketStatus("COMPLETED");
            order.setShiprocketError(null);

        } catch (Exception exception) {
            order.setShiprocketError(exception.getMessage());

            if (order.getShiprocketStatus() == null
                    || order.getShiprocketStatus().isBlank()) {
                order.setShiprocketStatus("FAILED");
            }

            throw new IllegalStateException(
                    "Shiprocket processing failed for order "
                            + order.getOrderNumber()
                            + ": " + exception.getMessage(),
                    exception
            );
        }
    }

    private Map<String, Object> postShiprocket(
            String endpoint,
            Map<String, Object> payload,
            String token
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    shiprocketConfig.getBaseUrl() + endpoint,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null
                    || response.getBody().isBlank()) {
                throw new IllegalStateException(
                        "Empty or unsuccessful response from " + endpoint
                );
            }

            return objectMapper.readValue(
                    response.getBody(),
                    objectMapper.getTypeFactory().constructMapType(
                            HashMap.class,
                            String.class,
                            Object.class
                    )
            );

        } catch (HttpStatusCodeException exception) {
            throw new IllegalStateException(
                    "Shiprocket API " + endpoint + " failed: "
                            + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to call Shiprocket API " + endpoint
                            + ": " + exception.getMessage(),
                    exception
            );
        }
    }

    @SuppressWarnings("unchecked")
    private String findResponseValue(
            Map<String, Object> response,
            String key
    ) {
        Object value = response.get(key);

        if (value != null && !value.toString().isBlank()) {
            return value.toString();
        }

        Object data = response.get("data");

        if (data instanceof Map<?, ?> dataMap) {
            Object nestedValue = dataMap.get(key);

            if (nestedValue != null && !nestedValue.toString().isBlank()) {
                return nestedValue.toString();
            }
        }

        return null;
    }


    public Map<String, Object> trackShipmentByAwb(String awbCode) {

        if (awbCode == null || awbCode.isBlank()) {
            throw new IllegalArgumentException(
                    "AWB code is required for shipment tracking."
            );
        }

        String token = getAuthToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = shiprocketConfig.getBaseUrl()
                + "/courier/track/awb/"
                + awbCode.trim();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null
                    || response.getBody().isBlank()) {
                throw new IllegalStateException(
                        "Shiprocket returned an empty tracking response."
                );
            }

            return objectMapper.readValue(
                    response.getBody(),
                    objectMapper.getTypeFactory().constructMapType(
                            HashMap.class,
                            String.class,
                            Object.class
                    )
            );

        } catch (HttpStatusCodeException exception) {
            throw new IllegalStateException(
                    "Shiprocket tracking failed: "
                            + exception.getResponseBodyAsString(),
                    exception
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to fetch shipment tracking: "
                            + exception.getMessage(),
                    exception
            );
        }
    }


}