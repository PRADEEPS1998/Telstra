package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceDetails {

    @JsonProperty("paymentInstallments")
    private String paymentInstallments;

    @JsonProperty("repaymentRetailTotal")
    private String repaymentRetailTotal;

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("deviceName")
    private String deviceName;

    @JsonProperty("noOfPayments")
    private String noOfPayments;

    @JsonProperty("deviceBasePrice")
    private String deviceBasePrice;

    @JsonProperty("salesChannel")
    private String salesChannel;

    @JsonProperty("remainingPayoutAmount")
    private String remainingPayoutAmount;

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("storage")
    private String storage;

    @JsonProperty("color")
    private String color;

    @JsonProperty("installmentsCompleted")
    private String installmentsCompleted;

    @JsonProperty("type")
    private String deviceType;

    @JsonProperty("deviceRepaymentCode")
    private String deviceRepaymentCode;

    @JsonProperty("mROStartDate")
    private String mROStartDate;
}
