package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class Outcome {
    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("statusMessage")
    private String statusMessage;

    @JsonProperty("outcomeName")
    private String outcomeName;

    private List<OutcomeResult> outcomeResults;

    @JsonProperty("error")
    private Error error;
}
