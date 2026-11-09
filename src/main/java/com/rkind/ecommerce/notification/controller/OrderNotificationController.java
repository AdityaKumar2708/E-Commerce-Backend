package com.rkind.ecommerce.notification.controller;

import com.rkind.ecommerce.notification.dto.request.AddNotificationEmailRequest;
import com.rkind.ecommerce.notification.dto.response.OrderNotificationResponse;
import com.rkind.ecommerce.notification.dto.request.UpdateNotificationRequest;
import com.rkind.ecommerce.notification.service.OrderNotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/order-notifications")
@RequiredArgsConstructor
public class OrderNotificationController {

    private final OrderNotificationService notificationService;

    // GET: Fetch notification settings and email recipients
    @GetMapping
    public ResponseEntity<OrderNotificationResponse> getSettings() {

        OrderNotificationResponse response =
                new OrderNotificationResponse(
                        notificationService.isNotificationEnabled(),
                        notificationService.getNotificationEmails()
                );

        return ResponseEntity.ok(response);
    }

    // PUT: Enable or disable order notifications
    @PutMapping
    public ResponseEntity<OrderNotificationResponse> updateSettings(
            @RequestBody UpdateNotificationRequest request
    ) {

        boolean enabled =
                notificationService.updateNotificationStatus(
                        request.enabled()
                );

        return ResponseEntity.ok(
                new OrderNotificationResponse(
                        enabled,
                        notificationService.getNotificationEmails()
                )
        );
    }

    // POST: Add a notification email
    @PostMapping("/emails")
    public ResponseEntity<OrderNotificationResponse> addEmail(
            @RequestBody AddNotificationEmailRequest request
    ) {

        notificationService.addNotificationEmail(request.email());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new OrderNotificationResponse(
                        notificationService.isNotificationEnabled(),
                        notificationService.getNotificationEmails()
                )
        );
    }

    // DELETE: Remove a notification email
    @DeleteMapping("/emails/{id}")
    public ResponseEntity<OrderNotificationResponse> deleteEmail(
            @PathVariable Long id
    ) {

        notificationService.removeNotificationEmail(id);

        return ResponseEntity.ok(
                new OrderNotificationResponse(
                        notificationService.isNotificationEnabled(),
                        notificationService.getNotificationEmails()
                )
        );
    }
}