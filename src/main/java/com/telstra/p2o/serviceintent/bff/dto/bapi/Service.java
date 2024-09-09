package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
@JsonInclude(Include.NON_NULL)
public class Service {
    @JsonProperty("businessContext")
    private BusinessContext businessContext;

    @JsonProperty("qualifiers")
    private List<Qualifier> qualifiers;

    @JsonProperty("outcomes")
    private List<Outcome> outcomes;
}
