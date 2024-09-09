package com.telstra.p2o.serviceintent.bff.dto.bffResponse;

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
public class DeviceIntentResponse {
    @JsonProperty("statusCode")
    private String statusCode;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("time")
    private String time;

    @JsonProperty("redeemingServiceData")
    private RedeemingServiceData redeemingServiceData;

    @JsonProperty("repaymentOptions")
    private RepaymentOptions repaymentOptions;
}
