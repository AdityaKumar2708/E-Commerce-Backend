
package com.rkind.ecommerce.address.controller;

import com.rkind.ecommerce.address.dto.request.CustomerAddressRequest;
import com.rkind.ecommerce.address.dto.response.CustomerAddressResponse;
import com.rkind.ecommerce.address.entity.CustomerAddress;
import com.rkind.ecommerce.address.service.CustomerAddressService;
import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.auth.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/customer/addresses")
public class CustomerAddressController {

    private final CustomerAddressService addressService;
    private final UserRepository userRepository;

    public CustomerAddressController(
            CustomerAddressService addressService,
            UserRepository userRepository
    ) {
        this.addressService = addressService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<CustomerAddressResponse> getAddresses(
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);

        return addressService.getAddresses(userId)
                .stream()
                .map(CustomerAddressResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerAddressResponse addAddress(
            Authentication authentication,
            @RequestBody CustomerAddressRequest request
    ) {
        Long userId = getAuthenticatedUserId(authentication);

        try {
            CustomerAddress saved = addressService.addAddress(
                    userId,
                    toEntity(request)
            );

            return CustomerAddressResponse.from(saved);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    @PutMapping("/{addressId}")
    public CustomerAddressResponse updateAddress(
            Authentication authentication,
            @PathVariable Long addressId,
            @RequestBody CustomerAddressRequest request
    ) {
        Long userId = getAuthenticatedUserId(authentication);

        try {
            CustomerAddress updated = addressService.updateAddress(
                    userId,
                    addressId,
                    toEntity(request)
            );

            return CustomerAddressResponse.from(updated);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    @DeleteMapping("/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(
            Authentication authentication,
            @PathVariable Long addressId
    ) {
        Long userId = getAuthenticatedUserId(authentication);

        try {
            addressService.deleteAddress(userId, addressId);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        }
    }

    private Long getAuthenticatedUserId(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please sign in first"
            );
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authenticated customer not found"
                        )
                );

        return user.getId();
    }

    private CustomerAddress toEntity(
            CustomerAddressRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Address details are required"
            );
        }

        CustomerAddress address = new CustomerAddress();

        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAlternatePhone(request.getAlternatePhone());
        address.setHouse(request.getHouse());
        address.setStreet(request.getStreet());
        address.setArea(request.getArea());
        address.setLandmark(request.getLandmark());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setAddressType(request.getAddressType());
        address.setDefault(request.isDefault());

        return address;
    }
}
