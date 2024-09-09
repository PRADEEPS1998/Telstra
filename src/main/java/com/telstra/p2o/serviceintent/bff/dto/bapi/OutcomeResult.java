package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.telstra.p2o.serviceintent.bff.constant.Constants;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutcomeResult {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.DD_MMM_YYYY)
    private Date date;

    private String description;
}
