package com.archvis.domain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DataSourceRequirement {
    @JacksonXmlProperty(isAttribute = true)
    private String datasourceId;

    @JacksonXmlProperty(isAttribute = true)
    private String collectionId;

    @JacksonXmlProperty(isAttribute = true)
    private String metricId;
}
