
package com.rkind.ecommerce.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import com.rkind.ecommerce.product.entity.SalesChannel;

public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Category is required")
    private String category;

    private String description;
    // Multiple product image URLs
    private List<String> imageUrls;

    private BigDecimal price;
    @jakarta.validation.constraints.NotNull(message = "Sales channel is required")
    private SalesChannel salesChannel = SalesChannel.ONLINE;

    // Internal purchase cost
    @NotNull(message = "Purchase cost is required")
    @PositiveOrZero(message = "Purchase cost cannot be negative")
    private BigDecimal purchaseCost;

    // Packaging cost per item
    @NotNull(message = "Packaging cost is required")
    @PositiveOrZero(message = "Packaging cost cannot be negative")
    private BigDecimal packagingCost = BigDecimal.ZERO;

    // Margin percentage configured by Admin
    @NotNull(message = "Margin percentage is required")
    @PositiveOrZero(message = "Margin percentage cannot be negative")
    private BigDecimal marginPercentage = new BigDecimal("5.00");

    private String imageUrl;

    @NotNull(message = "Stock is required")
    @PositiveOrZero(message = "Stock cannot be negative")
    private Integer stock;

    private String sku;

    private BigDecimal originalPrice;

    private Boolean active = true;

    // Shipping details
// Required only for ONLINE products
    private BigDecimal weight;

    private BigDecimal length;

    private BigDecimal width;

    private BigDecimal height;

    public ProductRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public SalesChannel getSalesChannel() {
        return salesChannel;
    }

    public void setSalesChannel(SalesChannel salesChannel) {
        this.salesChannel = salesChannel;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPurchaseCost() {
        return purchaseCost;
    }
    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public void setPurchaseCost(BigDecimal purchaseCost) {
        this.purchaseCost = purchaseCost;
    }

    public BigDecimal getPackagingCost() {
        return packagingCost;
    }

    public void setPackagingCost(BigDecimal packagingCost) {
        this.packagingCost = packagingCost;
    }

    public BigDecimal getMarginPercentage() {
        return marginPercentage;
    }

    public void setMarginPercentage(BigDecimal marginPercentage) {
        this.marginPercentage = marginPercentage;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getLength() {
        return length;
    }

    public void setLength(BigDecimal length) {
        this.length = length;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public void setWidth(BigDecimal width) {
        this.width = width;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }
}
