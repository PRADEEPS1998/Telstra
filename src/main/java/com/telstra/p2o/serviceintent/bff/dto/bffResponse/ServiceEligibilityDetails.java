package com.telstra.p2o.serviceintent.bff.dto.bffResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.telstra.p2o.serviceintent.bff.dto.bapi.ETCDetails;

public class ServiceEligibilityDetails {
    @JsonProperty("action")
    private String action;

    @JsonProperty("existingPlanDetails")
    private ExistingPlanDetails existingPlanDetails;

    @JsonProperty("selectedPlanDetails")
    private SelectedPlanDetails selectedPlanDetails;

    @JsonProperty("isFeasibleEligibleAndETCSuccess")
    private Boolean isFeasibleEligibleAndETCSuccess;

    @JsonProperty("redirectToMyT")
    private Boolean redirectToMyT;

    @JsonProperty("redirectToAgora")
    private Boolean redirectToAgora;

    @JsonProperty("agoraDeepLink")
    private String agoraDeepLink;

    @JsonProperty("isTONAvailable")
    private Boolean isTONAvailable;

    @JsonProperty("etcDetails")
    private ETCDetails etcDetails;
}
