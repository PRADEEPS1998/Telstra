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
public class FeasibilityOutcome {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("feasibilityStatus")
    private String feasibilityStatus;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("BillingAccountId")
    private String billingAccountId;

    @JsonProperty("messageBankType")
    private String messageBankType;

    @JsonProperty("simSerialNumber")
    private String simSerialNumber;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("isTON")
    private String isTON;

    @JsonProperty("directoryListingIndicator")
    private String directoryListingIndicator;

    @JsonProperty("callingLineIdentificationType")
    private String callingLineIdentificationType;

    @JsonProperty("errors")
    private SIBFFError errors;

    @JsonProperty("characteristic")
    private List<Characteristic> characteristic;

    @JsonProperty("stayConn")
    private Boolean stayConn;

    @JsonProperty("simCategory")
    private String simCategory;
}
