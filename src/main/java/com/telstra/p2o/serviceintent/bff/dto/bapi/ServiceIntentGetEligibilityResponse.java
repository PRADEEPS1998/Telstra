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
public class ServiceIntentGetEligibilityResponse {
    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("statusCode")
    private int statusCode;

    @JsonProperty("time")
    private String time;

    @JsonProperty("services")
    private List<Service> services;

    @JsonProperty("errors")
    List<Errors> errors;
}
