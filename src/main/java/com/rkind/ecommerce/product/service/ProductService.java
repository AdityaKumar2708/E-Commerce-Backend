
package com.rkind.ecommerce.product.service;

import com.rkind.ecommerce.order.repository.OrderItemRepository;
import com.rkind.ecommerce.product.dto.request.ProductRequest;
import com.rkind.ecommerce.product.dto.response.ProductResponse;
import com.rkind.ecommerce.product.entity.Product;
import com.rkind.ecommerce.product.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import com.rkind.ecommerce.product.entity.SalesChannel;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private static final BigDecimal PAYMENT_FEE_PERCENT =
            new BigDecimal("3.00");

    private static final BigDecimal PAYMENT_FEE_GST_PERCENT =
            new BigDecimal("18.00");

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");
    private final OrderItemRepository orderItemRepository;

    // Add Product
    public ProductResponse addProduct(ProductRequest request) {

        Product product = new Product();

        mapRequestToProduct(request, product);

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }

    // Get All Products
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Get Product By ID
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Product not found with ID: " + id
                ));

        return mapToResponse(product);
    }

    // Update Product
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Product not found with ID: " + id
                ));

        mapRequestToProduct(request, product);

        Product updatedProduct = productRepository.save(product);

        return mapToResponse(updatedProduct);
    }

    // Map Request DTO to Product Entity
    private void mapRequestToProduct(ProductRequest request, Product product) {

        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setDescription(request.getDescription());

        product.setSalesChannel(
                request.getSalesChannel() != null
                        ? request.getSalesChannel()
                        : SalesChannel.ONLINE
        );

        // Internal costs
        product.setPurchaseCost(request.getPurchaseCost());
        product.setPackagingCost(request.getPackagingCost());
        product.setMarginPercentage(request.getMarginPercentage());

        // Automatically calculate selling price
        product.setPrice(calculateSellingPrice(
                request.getPurchaseCost(),
                request.getPackagingCost(),
                request.getMarginPercentage()
        ));

        // Multiple product images
        List<String> imageUrls = request.getImageUrls() != null
                ? request.getImageUrls().stream()
                .filter(url -> url != null && !url.isBlank())
                .toList()
                : List.of();

        product.setImageUrls(imageUrls);

        // Keep first image as primary image for existing response support
        if (!imageUrls.isEmpty()) {
            product.setImageUrl(imageUrls.get(0));
        } else {
            product.setImageUrl(request.getImageUrl());
        }

        // Other product details
        product.setStock(request.getStock());
        product.setSku(request.getSku());
        product.setOriginalPrice(request.getOriginalPrice());

        product.setActive(
                request.getActive() != null ? request.getActive() : true
        );

        if (request.getSalesChannel() == SalesChannel.ONLINE) {

            if (request.getWeight() == null || request.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Weight is required for online products.");
            }

            if (request.getLength() == null || request.getLength().compareTo(new BigDecimal("0.5")) <= 0) {
                throw new IllegalArgumentException("Length must be greater than 0.5 cm for online products.");
            }

            if (request.getWidth() == null || request.getWidth().compareTo(new BigDecimal("0.5")) <= 0) {
                throw new IllegalArgumentException("Width must be greater than 0.5 cm for online products.");
            }

            if (request.getHeight() == null || request.getHeight().compareTo(new BigDecimal("0.5")) <= 0) {
                throw new IllegalArgumentException("Height must be greater than 0.5 cm for online products.");
            }

        } else if (request.getSalesChannel() == SalesChannel.STORE_PICKUP) {

            // Store pickup does not require shipping information
            request.setWeight(null);
            request.setLength(null);
            request.setWidth(null);
            request.setHeight(null);
        }

        // Shipping details for Shiprocket
        product.setWeight(request.getWeight());
        product.setLength(request.getLength());
        product.setWidth(request.getWidth());
        product.setHeight(request.getHeight());
    }

    // Calculate Selling Price
    private BigDecimal calculateSellingPrice(
            BigDecimal purchaseCost,
            BigDecimal packagingCost,
            BigDecimal marginPercentage) {

        // Margin calculated on purchase cost
        BigDecimal marginAmount = purchaseCost
                .multiply(marginPercentage)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);

        // Purchase cost + packaging cost + margin
        BigDecimal baseAmount = purchaseCost
                .add(packagingCost)
                .add(marginAmount);

        // Payment gateway fee
        BigDecimal paymentFee = baseAmount
                .multiply(PAYMENT_FEE_PERCENT)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);

        // GST on payment gateway fee
        BigDecimal paymentFeeGst = paymentFee
                .multiply(PAYMENT_FEE_GST_PERCENT)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);

        // Round final selling price to nearest rupee
        return baseAmount
                .add(paymentFee)
                .add(paymentFeeGst)
                .setScale(0, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.UNNECESSARY);
    }
    // Delete Product - safe handling for existing orders

    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Product not found with ID: " + id
                ));

        boolean usedInOrders = orderItemRepository.existsByProduct_Id(id);

        if (usedInOrders) {
            // Preserve historical order references.
            product.setActive(false);
            productRepository.save(product);
        } else {
            // No order references: safe to physically delete.
            productRepository.delete(product);
        }
    }


    // Entity to Customer Response DTO
    private ProductResponse mapToResponse(Product product) {

        ProductResponse response = new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getStock(),
                product.getSku(),
                product.getOriginalPrice(),
                product.getActive()
        );

        response.setImageUrls(product.getImageUrls());
        response.setWeight(product.getWeight());
        response.setLength(product.getLength());
        response.setWidth(product.getWidth());
        response.setHeight(product.getHeight());
        response.setSalesChannel(product.getSalesChannel());

        return response;
    }


}
