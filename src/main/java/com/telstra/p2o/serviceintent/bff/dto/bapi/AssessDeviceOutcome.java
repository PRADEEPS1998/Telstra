package com.telstra.p2o.serviceintent.bff.dto.bapi;

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
public class AssessDeviceOutcome {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("characteristics")
    private List<Characteristic> characteristics;

    private String statusMessage;

    private List<Questions> questionnaire;

    private List<Errors> errors;
}
