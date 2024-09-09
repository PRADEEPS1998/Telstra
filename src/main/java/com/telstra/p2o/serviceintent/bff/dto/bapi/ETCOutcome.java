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
public class ETCOutcome {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("serviceid")
    private String serviceId;

    @JsonProperty("serviceType")
    private String serviceType;

    @JsonProperty("hasETC")
    private Boolean hasETC;

    @JsonProperty("isCIMTransferRODevice")
    private Boolean isCIMTransferRODevice;

    @JsonProperty("deviceDetails")
    private List<DeviceDetails> deviceDetails;

    @JsonProperty("etcsAmount")
    private ETCSAmount etcsAmount;

    @JsonProperty("errors")
    private SIBFFError errors;

    @JsonProperty("characteristic")
    private List<Characteristic> characteristic;
}
