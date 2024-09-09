package com.telstra.p2o.serviceintent.bff.dto.bffRequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.telstra.p2o.serviceintent.bff.constant.Constants;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceDetails {

    @Valid
    @NotEmpty(message = Constants.DEVICE_DETAILS + Constants.NULL_ERR_MSG)
    private String deviceId;

    @Valid
    @NotEmpty(message = Constants.DEVICE_DETAILS + Constants.NULL_ERR_MSG)
    private String deviceType;

    private String dateOfPurchase;

    private String typeOfProof;
}
