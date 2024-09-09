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
public class ExistingPlanDetails {

    @JsonProperty("planName")
    private String planName;

    @JsonProperty("isROLinked")
    private Boolean isROLinked;

    @JsonProperty("deviceName")
    private String deviceName;

    @JsonProperty("sourceSystem")
    private String sourceSystem;

    @JsonProperty("hasDUP")
    private Boolean hasDUP;

    @JsonProperty("isPrepaidService")
    private Boolean isPrepaidService;
}
