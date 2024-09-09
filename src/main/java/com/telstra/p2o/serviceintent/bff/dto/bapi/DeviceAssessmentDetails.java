package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.telstra.p2o.serviceintent.bff.dto.bffRequest.AssessmentType;
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
public class DeviceAssessmentDetails {

    @JsonProperty("assessmentType")
    private AssessmentType assessmentType;

    @JsonProperty("outcomes")
    private List<AssessDeviceOutcome> outcomes;

    private RedeemingDeviceDetails deviceDetails;
}
