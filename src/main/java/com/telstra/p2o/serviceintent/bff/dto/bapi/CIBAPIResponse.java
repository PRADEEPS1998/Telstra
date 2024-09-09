package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFError;
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
public class CIBAPIResponse {

    @JsonProperty("externalId")
    private String correlationId;

    @JsonProperty("statusCode")
    private String statusCode;

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("time")
    private String time;

    @JsonProperty("productGroup")
    private List<ProductGroup> productGroup;

    @JsonProperty("errors")
    private List<SIBFFError> errors;
}
