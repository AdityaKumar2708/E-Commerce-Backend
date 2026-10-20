
package com.rkind.ecommerce.product.dto.response;

import java.math.BigDecimal;
import java.util.List;
import com.rkind.ecommerce.product.entity.SalesChannel;

public class ProductResponse {

    private Long id;
    private String name;
    private String category;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private List<String> imageUrls;
    private Integer stock;
    private String sku;
    private BigDecimal originalPrice;
    private BigDecimal weight;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private Boolean active;
    private SalesChannel salesChannel;

    public ProductResponse() {
    }

    public ProductResponse(
            Long id,
            String name,
            String category,
            String description,
            BigDecimal price,
            String imageUrl,
            Integer stock,
            String sku,
            BigDecimal originalPrice,
            Boolean active
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.stock = stock;
        this.sku = sku;
        this.originalPrice = originalPrice;
        this.active = active;
    }

    public Long getId() {
        return id;
    }
    public SalesChannel getSalesChannel() {
        return salesChannel;
    }

    public void setSalesChannel(SalesChannel salesChannel) {
        this.salesChannel = salesChannel;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
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

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getLength() { return length; }
    public void setLength(BigDecimal length) { this.length = length; }
    public BigDecimal getWidth() { return width; }
    public void setWidth(BigDecimal width) { this.width = width; }
    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
