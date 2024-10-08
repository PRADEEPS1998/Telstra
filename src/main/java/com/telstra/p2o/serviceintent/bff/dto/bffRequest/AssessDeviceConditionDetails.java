package com.telstra.p2o.serviceintent.bff.dto.bffRequest;

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
public class AssessDeviceConditionDetails {

    @JsonProperty("deviceCondition")
    private List<DeviceCondition> deviceCondition;

    private String deviceId;
}
