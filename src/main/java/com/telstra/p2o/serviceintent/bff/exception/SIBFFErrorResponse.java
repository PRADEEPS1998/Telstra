package com.telstra.p2o.serviceintent.bff.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"statusCode", "success", "errors"})
public class SIBFFErrorResponse {

    @JsonProperty("statusCode")
    private int statusCode;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("errors")
    private List<SIBFFError> errors;

    public List<SIBFFError> addError(String message, String code) {
        if (this.getErrors() != null) {
            errors.add(SIBFFError.builder().message(message).code(code).build());
        } else {
            this.errors = new ArrayList<>();
            errors.add(SIBFFError.builder().message(message).code(code).build());
        }
        return errors;
    }
}
