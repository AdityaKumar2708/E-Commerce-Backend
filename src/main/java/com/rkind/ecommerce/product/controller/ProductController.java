
package com.rkind.ecommerce.product.controller;

import com.rkind.ecommerce.product.dto.request.ProductRequest;
import com.rkind.ecommerce.product.dto.response.ProductResponse;
import com.rkind.ecommerce.product.service.ProductService;
import com.rkind.ecommerce.product.service.ProductImageService;

import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductImageService productImageService;

    public ProductController(
            ProductService productService,
            ProductImageService productImageService
    ) {
        this.productService = productService;
        this.productImageService = productImageService;
    }

    // Upload Multiple Product Images
    @PostMapping(
            value = "/images/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<String>> uploadImages(
            @RequestParam("files") List<MultipartFile> files
    ) throws IOException {

        System.out.println("🔥🔥🔥 UPLOAD CONTROLLER HIT 🔥🔥🔥");

        System.out.println("=================================");
        System.out.println("IMAGE UPLOAD API HIT");
        System.out.println("FILES COUNT = " + files.size());

        List<String> imageUrls =
                productImageService.uploadImages(files);

        System.out.println("UPLOADED URLS = " + imageUrls);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(imageUrls);
    }

    // Get Uploaded Product Image
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> getProductImage(
            @PathVariable String filename
    ) throws IOException {

        Path imagePath = productImageService.getImagePath(filename);
        Resource resource = new UrlResource(imagePath.toUri());

        String contentType = Files.probeContentType(imagePath);

        MediaType mediaType = contentType != null
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filename + "\""
                )
                .body(resource);
    }

    // Add Product
    @PostMapping
    public ResponseEntity<ProductResponse> addProduct(
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse response = productService.addProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get All Products
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {

        List<ProductResponse> products =
                productService.getAllProducts();

        return ResponseEntity.ok(products);
    }

    // Get Product By ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable Long id
    ) {
        ProductResponse response =
                productService.getProductById(id);

        return ResponseEntity.ok(response);
    }

    // Update Product
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse response =
                productService.updateProduct(id, request);

        return ResponseEntity.ok(response);
    }

    // Delete Product
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
