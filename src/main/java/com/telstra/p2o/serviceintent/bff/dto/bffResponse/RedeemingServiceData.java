package com.telstra.p2o.serviceintent.bff.dto.bffResponse;

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
public class RedeemingServiceData {
    @JsonProperty("serviceId")
    private String serviceId;

    @JsonProperty("plan")
    private PlanDetails plan;

    @JsonProperty("deviceName")
    private String deviceName;

    private List<Addon> addons;
}
