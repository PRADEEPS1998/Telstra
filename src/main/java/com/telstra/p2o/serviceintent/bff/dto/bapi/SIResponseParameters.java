package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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
public class SIResponseParameters {

    @JsonProperty("isFeasible")
    private boolean isFeasible;

    @JsonProperty("isTONAvailable")
    private boolean isTONAvailable;

    @JsonProperty("isEligibile")
    private boolean isEligibile;

    @JsonProperty("isETCSuccess")
    private boolean isETCSuccess;

    @JsonProperty("hasETC")
    private boolean hasETC;

    @JsonProperty("isCIMTransferRODevice")
    private boolean isCIMTransferRODevice;

    @JsonProperty("etcDeviceDetails")
    private List<DeviceDetails> etcDeviceDetails;

    @JsonProperty("etcsAmount")
    private ETCSAmount etcsAmount;

    @JsonProperty("isAgoraLinkFound")
    private boolean isAgoraLinkFound;

    @JsonProperty("isFeasibleEligibleAndETCSuccess")
    private boolean isFeasibleEligibleAndETCSuccess;

    @JsonProperty("reasonForFeasibilityError")
    private String reasonForFeasibilityError;
}
