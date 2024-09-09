package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
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
public class DeviceAssessmentResponse {

    @JsonProperty("statusCode")
    private String statusCode;

    @JsonProperty("time")
    private Date time;

    @JsonProperty("deviceAssessment")
    private List<DeviceAssessmentDetails> deviceAssessment;
}
