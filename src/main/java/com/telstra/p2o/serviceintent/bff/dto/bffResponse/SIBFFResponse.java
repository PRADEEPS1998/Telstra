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
public class SIBFFResponse {

    @JsonProperty("statusCode")
    private String statusCode;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("time")
    private String time;

    @JsonProperty("action")
    private String action;

    @JsonProperty("existingPlanDetails")
    private ExistingPlanDetails existingPlanDetails;

    @JsonProperty("selectedPlanDetails")
    private SelectedPlanDetails selectedPlanDetails;

    @JsonProperty("isFeasibleEligibleAndETCSuccess")
    private Boolean isFeasibleEligibleAndETCSuccess;

    @JsonProperty("agoraDeepLink")
    private String agoraDeepLink;

    @JsonProperty("isTONAvailable")
    private Boolean isTONAvailable;

    @JsonProperty("stayConn")
    private Boolean stayConn;

    @JsonProperty("etcDetails")
    private ETCDetails etcDetails;

    @JsonProperty("repaymentOptions")
    private RepaymentOptions repaymentOptions;

    @JsonProperty("reasonForFeasibilityError")
    private String reasonForFeasibilityError;

    @JsonProperty("data")
    private com.telstra.p2o.serviceintent.bff.dto.bffResponse.Data data;
}
