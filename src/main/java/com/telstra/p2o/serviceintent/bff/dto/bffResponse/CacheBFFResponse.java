package com.telstra.p2o.serviceintent.bff.dto.bffResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CacheBFFResponse {

    @JsonProperty("statusCode")
    private int statusCode;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("time")
    private String time;

    @JsonProperty("externalId")
    private String externalId;
}
