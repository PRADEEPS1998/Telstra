package com.telstra.p2o.serviceintent.bff.dto.bapi;

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
public class CIResponseParameters {

    @JsonProperty("sourceSystem")
    private String sourceSystem;

    @JsonProperty("existingPlanName")
    private String existingPlanName;

    @JsonProperty("selectedPlanName")
    private String selectedPlanName;

    @JsonProperty("paymentMode")
    private String paymentMode;

    @JsonProperty("isROLinked")
    private boolean isROLinked;

    @JsonProperty("deviceName")
    private String deviceName;

    @JsonProperty("productSubType")
    private String productSubType;

    @JsonProperty("isDavinci")
    private boolean isDavinci;

    @JsonProperty("isMICA")
    private boolean isMICA;

    @JsonProperty("isPrepaidService")
    private boolean isPrepaidService;

    @JsonProperty("isPlanInCart")
    private boolean isPlanInCart;

    @JsonProperty("isDeviceInCart")
    private boolean isDeviceInCart;

    @JsonProperty("isDeviceROInCart")
    private boolean isDeviceROInCart;

    @JsonProperty("contractAssetReferenceId")
    private String contractAssetReferenceId;
}
