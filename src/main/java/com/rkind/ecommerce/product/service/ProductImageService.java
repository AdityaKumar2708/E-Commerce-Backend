
package com.rkind.ecommerce.product.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductImageService {

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    public List<String> uploadImages(List<MultipartFile> files)
            throws IOException {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select at least one image");
        }

        if (cloudName == null || cloudName.isBlank()
                || apiKey == null || apiKey.isBlank()
                || apiSecret == null || apiSecret.isBlank()) {
            throw new IOException(
                    "Cloudinary configuration is missing");
        }

        Cloudinary cloudinary = new Cloudinary(
                ObjectUtils.asMap(
                        "cloud_name", cloudName,
                        "api_key", apiKey,
                        "api_secret", apiSecret,
                        "secure", true
                )
        );

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException(
                        "An uploaded image is empty");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException(
                        "Each image must be 10 MB or smaller");
            }

            String contentType = file.getContentType();

            if (contentType == null ||
                    !List.of(
                            "image/jpeg",
                            "image/png",
                            "image/webp",
                            "image/gif"
                    ).contains(contentType.toLowerCase())) {
                throw new IllegalArgumentException(
                        "Only JPG, PNG, WEBP and GIF images are allowed");
            }

            try {
                Map<?, ?> result = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "rajesh-stationery/products",
                                "resource_type", "image",
                                "public_id", UUID.randomUUID().toString()
                        )
                );

                Object secureUrl = result.get("secure_url");

                if (secureUrl == null) {
                    throw new IOException(
                            "Cloudinary did not return an image URL");
                }

                imageUrls.add(secureUrl.toString());

            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(
                        "Cloudinary image upload failed: "
                                + e.getMessage(), e);
            }
        }

        return imageUrls;
    }

    // Legacy endpoint support for previously stored local images
    public Path getImagePath(String filename) throws IOException {

        if (filename == null ||
                !filename.matches("[a-f0-9-]+\\.(jpg|png|webp|gif)")) {
            throw new IllegalArgumentException(
                    "Invalid image filename");
        }

        Path uploadPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();

        Path imagePath = uploadPath.resolve(filename).normalize();

        if (!imagePath.startsWith(uploadPath)
                || !Files.exists(imagePath)
                || !Files.isRegularFile(imagePath)
                || !Files.isReadable(imagePath)) {
            throw new IOException("Image not found");
        }

        return imagePath;
    }
}
