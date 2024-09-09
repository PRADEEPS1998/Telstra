package com.telstra.p2o.serviceintent.bff.dto.bapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductGroup {

    @JsonProperty("id")
    private String id;

    @JsonProperty("@type")
    private String type;

    @JsonProperty("sourceSystem")
    private String sourceSystem;

    @JsonProperty("group")
    private String group;

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("product")
    private List<Product> product;
}
