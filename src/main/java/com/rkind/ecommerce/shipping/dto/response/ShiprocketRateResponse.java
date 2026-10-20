package com.rkind.ecommerce.shipping.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class ShiprocketRateResponse {

    private String currency;

    private Data data;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        private List<CourierCompany> available_courier_companies;

        public List<CourierCompany> getAvailable_courier_companies() {
            return available_courier_companies;
        }

        public void setAvailable_courier_companies(
                List<CourierCompany> available_courier_companies) {
            this.available_courier_companies = available_courier_companies;
        }
    }

    public static class CourierCompany {

        private Integer courier_company_id;

        private String courier_name;

        private BigDecimal rate;

        private BigDecimal freight_charge;

        private BigDecimal charge_weight;

        private String estimated_delivery_days;

        private String etd;

        private BigDecimal cod_charges;

        public Integer getCourier_company_id() {
            return courier_company_id;
        }

        public void setCourier_company_id(Integer courier_company_id) {
            this.courier_company_id = courier_company_id;
        }

        public String getCourier_name() {
            return courier_name;
        }

        public void setCourier_name(String courier_name) {
            this.courier_name = courier_name;
        }

        public BigDecimal getRate() {
            return rate;
        }

        public void setRate(BigDecimal rate) {
            this.rate = rate;
        }

        public BigDecimal getFreight_charge() {
            return freight_charge;
        }

        public void setFreight_charge(BigDecimal freight_charge) {
            this.freight_charge = freight_charge;
        }

        public BigDecimal getCharge_weight() {
            return charge_weight;
        }

        public void setCharge_weight(BigDecimal charge_weight) {
            this.charge_weight = charge_weight;
        }

        public String getEstimated_delivery_days() {
            return estimated_delivery_days;
        }

        public void setEstimated_delivery_days(String estimated_delivery_days) {
            this.estimated_delivery_days = estimated_delivery_days;
        }

        public String getEtd() {
            return etd;
        }

        public void setEtd(String etd) {
            this.etd = etd;
        }

        public BigDecimal getCod_charges() {
            return cod_charges;
        }

        public void setCod_charges(BigDecimal cod_charges) {
            this.cod_charges = cod_charges;
        }
    }
}