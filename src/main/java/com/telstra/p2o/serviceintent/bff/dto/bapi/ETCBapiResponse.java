package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFError;
import java.util.Date;
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
public class ETCBapiResponse {

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("statusCode")
    private String statusCode;

    @JsonProperty("time")
    private Date time;

    @JsonProperty("getETCResponse")
    private GetETCResponse getETCResponse;

    @JsonProperty("errors")
    private List<SIBFFError> errors;
}
