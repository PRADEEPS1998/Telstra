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
public class AssessDeviceData {

    @JsonProperty("assessmentResult")
    private String assessmentResult;

    @JsonProperty("redemptionFee")
    private String redemptionFee;

    @JsonProperty("deviceAssessedFor")
    private AssessDeviceConditionPayload deviceAssessedFor;

    @JsonProperty("deviceDetails")
    private AssessDeviceDetails deviceDetails;
}
