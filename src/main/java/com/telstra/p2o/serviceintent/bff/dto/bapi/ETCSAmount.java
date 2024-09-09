package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class ETCSAmount {

    @JsonProperty("planETC")
    private String planETC;

    @JsonProperty("deviceETC")
    private String deviceETC;

    @JsonProperty("totalETC")
    private String totalETC;

    @JsonProperty("otherETC")
    private String otherETC;
}
