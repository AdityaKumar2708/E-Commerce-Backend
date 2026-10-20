
package com.rkind.ecommerce.product.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ProductImageService {

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public List<String> uploadImages(List<MultipartFile> files)
            throws IOException {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Please select at least one image");
        }

        Path uploadPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(uploadPath);

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("An uploaded image is empty");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException(
                        "Each image must be 10 MB or smaller"
                );
            }

            String contentType = file.getContentType();

            String extension;
            if ("image/jpeg".equalsIgnoreCase(contentType)) {
                extension = ".jpg";
            } else if ("image/png".equalsIgnoreCase(contentType)) {
                extension = ".png";
            } else if ("image/webp".equalsIgnoreCase(contentType)) {
                extension = ".webp";
            } else if ("image/gif".equalsIgnoreCase(contentType)) {
                extension = ".gif";
            } else {
                throw new IllegalArgumentException(
                        "Only JPG, PNG, WEBP and GIF images are allowed"
                );
            }

            String filename = UUID.randomUUID() + extension;
            Path targetPath = uploadPath.resolve(filename).normalize();

            if (!targetPath.startsWith(uploadPath)) {
                throw new SecurityException("Invalid image path");
            }

            Files.copy(file.getInputStream(), targetPath);

            imageUrls.add("/api/products/images/" + filename);
        }

        return imageUrls;
    }

    public Path getImagePath(String filename) throws IOException {

        if (filename == null ||
                !filename.matches("[a-f0-9-]+\\.(jpg|png|webp|gif)")) {
            throw new IllegalArgumentException("Invalid image filename");
        }

        Path uploadPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        Path imagePath = uploadPath.resolve(filename).normalize();

        if (!imagePath.startsWith(uploadPath) ||
                !Files.exists(imagePath) ||
                !Files.isRegularFile(imagePath) ||
                !Files.isReadable(imagePath)) {
            throw new IOException("Image not found");
        }

        return imagePath;
    }
}
