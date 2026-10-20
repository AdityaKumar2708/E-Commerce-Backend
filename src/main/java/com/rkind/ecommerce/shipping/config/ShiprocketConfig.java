
package com.rkind.ecommerce.shipping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "shiprocket")
public class ShiprocketConfig {

    private String baseUrl;
    private String email;
    private String password;
    private String pickupPincode;
    private String pickupLocation;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPickupPincode() {
        return pickupPincode;
    }

    public void setPickupPincode(String pickupPincode) {
        this.pickupPincode = pickupPincode;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }
}
