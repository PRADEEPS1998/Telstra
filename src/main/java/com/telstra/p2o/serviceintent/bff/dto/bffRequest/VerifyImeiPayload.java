package com.telstra.p2o.serviceintent.bff.dto.bffRequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.telstra.p2o.serviceintent.bff.constant.Constants;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerifyImeiPayload {

    @Valid
    @NotEmpty(message = Constants.VALIDATION_TYPE + Constants.NULL_ERR_MSG)
    private String validationType;

    @Valid
    @NotNull(message = Constants.DEVICE_DETAILS + Constants.NULL_ERR_MSG)
    private DeviceDetails deviceDetails;
}
