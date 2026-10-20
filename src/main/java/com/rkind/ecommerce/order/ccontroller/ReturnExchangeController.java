
package com.rkind.ecommerce.order.ccontroller;

import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.auth.repository.UserRepository;
import com.rkind.ecommerce.order.dto.request.ReturnExchangeRequestDto;
import com.rkind.ecommerce.order.entity.ReturnExchangeRequest;
import com.rkind.ecommerce.order.service.ReturnExchangeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReturnExchangeController {

    private final ReturnExchangeService returnExchangeService;
    private final UserRepository userRepository;

    // CUSTOMER: Submit a return/exchange request
    @PostMapping("/customer/orders/{orderId}/return-exchange")
    public ResponseEntity<ReturnExchangeRequest> createRequest(
            @PathVariable Long orderId,
            @Valid @RequestBody ReturnExchangeRequestDto dto,
            Authentication authentication) {

        User customer = getAuthenticatedUser(authentication);

        ReturnExchangeRequest request =
                returnExchangeService.createRequest(
                        orderId,
                        customer,
                        dto
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    // CUSTOMER: View own return/exchange requests
    @GetMapping("/customer/return-exchange")
    public ResponseEntity<List<ReturnExchangeRequest>> getMyRequests(
            Authentication authentication) {

        User customer = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                returnExchangeService.getMyRequests(customer.getId())
        );
    }

    // ADMIN: View requests awaiting review
    @GetMapping("/admin/return-exchange/pending")
    public ResponseEntity<List<ReturnExchangeRequest>> getPendingRequests() {

        return ResponseEntity.ok(
                returnExchangeService.getPendingRequests()
        );
    }

    // ADMIN: Approve a request
    @PostMapping("/admin/return-exchange/{requestId}/approve")
    public ResponseEntity<ReturnExchangeRequest> approve(
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body) {

        String adminNotes = body == null ? null : body.get("adminNotes");

        return ResponseEntity.ok(
                returnExchangeService.approve(requestId, adminNotes)
        );
    }

    // ADMIN: Reject a request with a reason
    @PostMapping("/admin/return-exchange/{requestId}/reject")
    public ResponseEntity<ReturnExchangeRequest> reject(
            @PathVariable Long requestId,
            @RequestBody Map<String, String> body) {

        String reason = body == null ? null : body.get("rejectionReason");

        return ResponseEntity.ok(
                returnExchangeService.reject(requestId, reason)
        );
    }

    private User getAuthenticatedUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required."
            );
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."
                ));
    }
}
