package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.RepaymentOptions;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetETCResponse {

    @JsonProperty("accountUUID")
    private String accountUUID;

    @JsonProperty("success")
    private String success;

    @JsonProperty("repaymentOptions")
    private List<RepaymentOptions> repaymentOptions;

    @JsonProperty("characteristic")
    private List<Characteristic> characteristic;
}
