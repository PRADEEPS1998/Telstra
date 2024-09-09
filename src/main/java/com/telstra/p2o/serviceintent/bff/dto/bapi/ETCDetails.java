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
public class ETCDetails {

    @JsonProperty("hasETC")
    private Boolean hasETC;

    @JsonProperty("isCIMTransferRODevice")
    private Boolean isCIMTransferRODevice;

    @JsonProperty("etcsAmount")
    private ETCSAmount etcsAmount;

    @JsonProperty("deviceDetails")
    private List<DeviceDetails> deviceDetails;
}
