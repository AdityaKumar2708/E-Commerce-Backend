
package com.rkind.ecommerce.address.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerAddressRequest {

    private String fullName;
    private String phone;
    private String alternatePhone;
    private String house;
    private String street;
    private String area;
    private String landmark;
    private String city;
    private String state;
    private String pincode;
    private String addressType;
    private boolean isDefault;
}
