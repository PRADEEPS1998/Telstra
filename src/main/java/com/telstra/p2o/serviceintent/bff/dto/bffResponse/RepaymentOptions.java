package com.telstra.p2o.serviceintent.bff.dto.bffResponse;

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
public class RepaymentOptions {

    @JsonProperty("assetReferenceId")
    private String assetReferenceId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("totalInstallments")
    private String totalInstallments;

    @JsonProperty("installmentsCompleted")
    private String installmentsCompleted;

    @JsonProperty("installmentsLeft")
    private String installmentsLeft;

    @JsonProperty("deviceName")
    private String deviceName;

    @JsonProperty("chargePointId")
    private String chargePointId;

    @JsonProperty("monthlyRoAmount")
    private String monthlyRoAmount;

    @JsonProperty("remainingPayoutAmount")
    private String remainingPayoutAmount;

    @JsonProperty("retailPrice")
    private String retailPrice;
}
