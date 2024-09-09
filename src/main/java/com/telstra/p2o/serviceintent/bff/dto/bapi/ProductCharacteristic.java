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
public class ProductCharacteristic {

    @JsonProperty("dataTier")
    private String dataTier;

    @JsonProperty("paymentAgreementId")
    private String paymentAgreementId;

    @JsonProperty("paymentMode")
    private String paymentMode;

    @JsonProperty("isDomesticExcessDataPolicy")
    private boolean isDomesticExcessDataPolicy;

    @JsonProperty("cis")
    private String cis;

    @JsonProperty("recurringAmount")
    private long recurringAmount;

    @JsonProperty("serviceId")
    private String serviceId;

    @JsonProperty("addressId")
    private String addressId;

    @JsonProperty("directoryListingType")
    private String directoryListingType;

    @JsonProperty("callLineIdentificationType")
    private String callLineIdentificationType;

    @JsonProperty("simSerialNumber")
    private String simSerialNumber;

    @JsonProperty("serialNumber")
    private String serialNumber;

    @JsonProperty("IMEI")
    private String imei;

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("SKU")
    private String sku;

    @JsonProperty("storage")
    private String storage;

    @JsonProperty("colour")
    private String colour;

    @JsonProperty("purchaseType")
    private String purchaseType;

    @JsonProperty("itemCondition")
    private String itemCondition;

    @JsonProperty("deviceName")
    private String deviceName;

    @JsonProperty("numberOfRepayments")
    private int numberOfRepayments;

    @JsonProperty("repaymentAmount")
    private long repaymentAmount;
}
