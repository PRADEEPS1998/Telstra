package com.telstra.p2o.serviceintent.bff.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class SIBFFException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SIBFFException(int statusCode, String message, String code) {
        super(message);
        List<SIBFFError> error = new ArrayList<>();
        error.add(SIBFFError.builder().message(message).code(code).build());
        this.errors = error;
        this.statusCode = statusCode;
        this.success = false;
    }

    @JsonProperty("errors")
    private final List<SIBFFError> errors;

    @JsonProperty("statusCode")
    private final int statusCode;

    @JsonProperty("success")
    private final Boolean success;
}
