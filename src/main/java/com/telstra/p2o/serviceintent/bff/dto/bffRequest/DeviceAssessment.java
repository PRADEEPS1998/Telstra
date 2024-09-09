package com.telstra.p2o.serviceintent.bff.dto.bffRequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.telstra.p2o.serviceintent.bff.dto.bapi.Characteristic;
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
public class DeviceAssessment {

    @JsonProperty("caseId")
    private String caseId;

    @JsonProperty("assessmentType")
    private AssessmentType assessmentType;

    @JsonProperty("characteristics")
    private List<Characteristic> characteristics;

    @JsonProperty("deviceDetails")
    private AssessDeviceConditionDetails deviceDetails;
}
