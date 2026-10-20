package com.rkind.ecommerce.account.service;

import com.rkind.ecommerce.account.dto.response.AccountResponse;
import com.rkind.ecommerce.account.dto.request.UpdateAccountRequest;
import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.auth.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final CloudinaryImageService imageService;

    public AccountService(
            UserRepository userRepository,
            CloudinaryImageService imageService
    ) {
        this.userRepository = userRepository;
        this.imageService = imageService;
    }

    @Transactional(readOnly = true)
    public AccountResponse getMyAccount(String authenticatedEmail) {
        User user = findUser(authenticatedEmail);
        return toResponse(user);
    }

    @Transactional
    public AccountResponse updateMyAccount(
            String authenticatedEmail,
            UpdateAccountRequest request
    ) {
        User user = findUser(authenticatedEmail);

        user.setFullName(request.fullName().trim());

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    @Transactional
    public AccountResponse updateProfileImage(
            String authenticatedEmail,
            MultipartFile file
    ) {
        User user = findUser(authenticatedEmail);

        String imageUrl = imageService.uploadProfileImage(
                file,
                user.getId()
        );

        user.setProfileImageUrl(imageUrl);

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Account not found or disabled"
                ));
    }

    private AccountResponse toResponse(User user) {
        return new AccountResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}