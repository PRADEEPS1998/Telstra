package com.telstra.p2o.serviceintent.bff.dto.bffResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HeadersData {

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("microToken")
    private String microToken;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("sourceSystem")
    private String sourceSystem;

    @JsonProperty("flowName")
    private String flowName;

    @JsonProperty("claims")
    private Map<String, Object> claims;
}
