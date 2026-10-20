package com.rkind.ecommerce.account.controller;

import com.rkind.ecommerce.account.dto.response.AccountResponse;
import com.rkind.ecommerce.account.dto.request.UpdateAccountRequest;
import com.rkind.ecommerce.account.service.AccountService;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // TEMPORARY: local testing only
    private static final String TEST_EMAIL = "aditya@scentmatic.in";

    @GetMapping("/me")
    public AccountResponse getMyAccount() {
        return accountService.getMyAccount(TEST_EMAIL);
    }

    @PatchMapping("/me")
    public AccountResponse updateMyAccount(
            @Valid @RequestBody UpdateAccountRequest request) {
        return accountService.updateMyAccount(TEST_EMAIL, request);
    }

    @PostMapping(
            value = "/me/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public AccountResponse uploadProfileImage(
            @RequestParam("file") MultipartFile file) {
        return accountService.updateProfileImage(TEST_EMAIL, file);
    }
}