package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class RedeemingDeviceDetails {

    private String deviceId;

    private String productId;

    private String deviceMake;

    private String deviceModel;

    private String size;

    private Boolean telstraDevice;

    private Boolean autoLockCheck;

    private String color;
}
