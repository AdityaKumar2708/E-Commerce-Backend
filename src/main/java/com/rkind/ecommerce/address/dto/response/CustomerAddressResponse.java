
package com.rkind.ecommerce.address.dto.response;

import com.rkind.ecommerce.address.entity.CustomerAddress;
import java.time.LocalDateTime;

public record CustomerAddressResponse(
        Long id,
        String fullName,
        String phone,
        String alternatePhone,
        String house,
        String street,
        String area,
        String landmark,
        String city,
        String state,
        String pincode,
        String addressType,
        boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CustomerAddressResponse from(CustomerAddress address) {
        return new CustomerAddressResponse(
                address.getId(),
                address.getFullName(),
                address.getPhone(),
                address.getAlternatePhone(),
                address.getHouse(),
                address.getStreet(),
                address.getArea(),
                address.getLandmark(),
                address.getCity(),
                address.getState(),
                address.getPincode(),
                address.getAddressType(),
                address.isDefault(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
