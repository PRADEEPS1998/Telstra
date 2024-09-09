package com.telstra.p2o.serviceintent.bff.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"code", "message"})
public class SIBFFError implements Serializable {

    private static final long serialVersionUID = 1905122041950251207L;

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;
}
