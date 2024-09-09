package com.telstra.p2o.serviceintent.bff.dto.bapi;

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
public class BusinessContext {
    @JsonProperty("family")
    private String family;

    @JsonProperty("type")
    private String type;

    @JsonProperty("subType")
    private String subType;

    @JsonProperty("businessAction")
    private String businessAction;

    @JsonProperty("serviceId")
    private String serviceId;
}
