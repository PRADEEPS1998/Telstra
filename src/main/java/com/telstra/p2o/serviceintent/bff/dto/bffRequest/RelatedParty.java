package com.telstra.p2o.serviceintent.bff.dto.bffRequest;

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
public class RelatedParty {

    @JsonProperty("id")
    private String id;

    @JsonProperty("role")
    private String role;

    @JsonProperty("@type")
    private String type;

    @JsonProperty("@referredType")
    private String referredType;
}
