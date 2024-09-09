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
public class EligibilityOutcome {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("eligibilityStatus")
    private String eligibilityStatus;

    @JsonProperty("agoraDeepLink")
    private String agoraDeepLink;

    @JsonProperty("isAddOnFound")
    private boolean isAddOnFound;

    @JsonProperty("errors")
    private SIBFFError errors;

    @JsonProperty("characteristic")
    private List<Characteristic> characteristic;
}
