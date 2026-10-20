package com.rkind.ecommerce.account.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryImageService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryImageService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret
    ) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public String uploadProfileImage(
            MultipartFile file,
            Long userId
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please select a profile image"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Image must be 5 MB or smaller"
            );
        }

        String contentType = file.getContentType();

        if (!"image/jpeg".equalsIgnoreCase(contentType)
                && !"image/png".equalsIgnoreCase(contentType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only JPG and PNG images are allowed"
            );
        }

        final byte[] bytes;

        try {
            bytes = file.getBytes();

            BufferedImage image = ImageIO.read(
                    new ByteArrayInputStream(bytes)
            );

            if (image == null
                    || image.getWidth() > 6000
                    || image.getHeight() > 6000) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid image or image dimensions are too large"
                );
            }

        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to read image file"
            );
        }

        if (cloudName.isBlank()
                || apiKey.isBlank()
                || apiSecret.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Profile image storage is not configured"
            );
        }

        try {
            Cloudinary cloudinary = new Cloudinary(
                    ObjectUtils.asMap(
                            "cloud_name", cloudName,
                            "api_key", apiKey,
                            "api_secret", apiSecret,
                            "secure", true
                    )
            );

            Map<?, ?> result = cloudinary.uploader().upload(
                    bytes,
                    ObjectUtils.asMap(
                            "resource_type", "image",
                            "folder", "rajesh-stationery/profiles",
                            "public_id", "user-" + userId,
                            "overwrite", true,
                            "invalidate", true
                    )
            );

            Object secureUrl = result.get("secure_url");

            if (secureUrl == null) {
                throw new IllegalStateException(
                        "Image storage did not return a secure URL"
                );
            }

            return secureUrl.toString();

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Profile image upload failed. Please try again."
            );
        }
    }
}