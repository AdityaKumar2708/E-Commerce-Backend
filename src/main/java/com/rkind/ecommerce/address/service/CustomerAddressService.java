
package com.rkind.ecommerce.address.service;

import com.rkind.ecommerce.address.entity.CustomerAddress;
import com.rkind.ecommerce.address.repository.CustomerAddressRepository;
import com.rkind.ecommerce.auth.entity.Role;
import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.auth.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerAddressService {

    private final CustomerAddressRepository addressRepository;
    private final UserRepository userRepository;

    public CustomerAddressService(
            CustomerAddressRepository addressRepository,
            UserRepository userRepository
    ) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerAddress> getAddresses(Long userId) {
        requireCustomer(userId);

        return addressRepository
                .findByUser_IdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    @Transactional
    public CustomerAddress addAddress(
            Long userId,
            CustomerAddress request
    ) {
        User user = requireCustomer(userId);

        validateAddress(request);

        request.setId(null);
        request.setUser(user);

        boolean firstAddress = !addressRepository.existsByUser_Id(userId);

        if (firstAddress || request.isDefault()) {
            makeDefaultAddress(userId);
            request.setDefault(true);
        }

        return addressRepository.save(request);
    }

    @Transactional
    public CustomerAddress updateAddress(
            Long userId,
            Long addressId,
            CustomerAddress request
    ) {
        requireCustomer(userId);
        validateAddress(request);

        CustomerAddress existing = addressRepository
                .findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Address not found"
                        )
                );

        existing.setFullName(request.getFullName().trim());
        existing.setPhone(request.getPhone().trim());
        existing.setAlternatePhone(
                blankToNull(request.getAlternatePhone())
        );
        existing.setHouse(request.getHouse().trim());
        existing.setStreet(blankToNull(request.getStreet()));
        existing.setArea(request.getArea().trim());
        existing.setLandmark(blankToNull(request.getLandmark()));
        existing.setCity(request.getCity().trim());
        existing.setState(request.getState().trim());
        existing.setPincode(request.getPincode().trim());
        existing.setAddressType(request.getAddressType().trim());

        if (request.isDefault()) {
            makeDefaultAddress(userId);
            existing.setDefault(true);
        }

        return addressRepository.save(existing);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        requireCustomer(userId);

        CustomerAddress existing = addressRepository
                .findByIdAndUser_Id(addressId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Address not found"
                        )
                );

        boolean wasDefault = existing.isDefault();

        addressRepository.delete(existing);
        addressRepository.flush();

        if (wasDefault) {
            List<CustomerAddress> remaining =
                    addressRepository
                            .findByUser_IdOrderByIsDefaultDescCreatedAtDesc(
                                    userId
                            );

            if (!remaining.isEmpty()) {
                remaining.get(0).setDefault(true);
                addressRepository.save(remaining.get(0));
            }
        }
    }

    private User requireCustomer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found"
                        )
                );

        if (user.getRole() != Role.CUSTOMER
                || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException(
                    "Active customer account required"
            );
        }

        return user;
    }

    private void makeDefaultAddress(Long userId) {
        List<CustomerAddress> addresses =
                addressRepository
                        .findByUser_IdOrderByIsDefaultDescCreatedAtDesc(
                                userId
                        );

        for (CustomerAddress address : addresses) {
            if (address.isDefault()) {
                address.setDefault(false);
            }
        }

        addressRepository.saveAll(addresses);
    }

    private void validateAddress(CustomerAddress address) {
        if (address == null
                || isBlank(address.getFullName())
                || isBlank(address.getPhone())
                || isBlank(address.getHouse())
                || isBlank(address.getArea())
                || isBlank(address.getCity())
                || isBlank(address.getState())
                || isBlank(address.getPincode())) {
            throw new IllegalArgumentException(
                    "Please fill all required address fields"
            );
        }

        if (!address.getPhone().trim().matches("^[6-9][0-9]{9}$")) {
            throw new IllegalArgumentException(
                    "Enter a valid 10-digit mobile number"
            );
        }

        if (!isBlank(address.getAlternatePhone())
                && !address.getAlternatePhone()
                .trim().matches("^[6-9][0-9]{9}$")) {
            throw new IllegalArgumentException(
                    "Enter a valid alternate mobile number"
            );
        }

        if (!address.getPincode().trim().matches("^[0-9]{6}$")) {
            throw new IllegalArgumentException(
                    "PIN code must contain exactly 6 digits"
            );
        }

        if (isBlank(address.getAddressType())
                || !List.of("Home", "Work")
                .contains(address.getAddressType().trim())) {
            throw new IllegalArgumentException(
                    "Address type must be Home or Work"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
