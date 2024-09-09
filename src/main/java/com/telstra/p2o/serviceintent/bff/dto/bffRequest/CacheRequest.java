package com.telstra.p2o.serviceintent.bff.dto.bffRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CacheRequest {
    @JsonProperty("action")
    private String action;
}
