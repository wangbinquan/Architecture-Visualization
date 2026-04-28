package com.archvis.domain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class FeatureConfig {
    @JacksonXmlProperty(isAttribute = true)
    private String id;

    @JacksonXmlProperty(isAttribute = true)
    private String name;

    @JacksonXmlElementWrapper(localName = "databaseTables")
    @JacksonXmlProperty(localName = "table")
    private List<DatabaseTable> databaseTables = new ArrayList<>();

    @JacksonXmlElementWrapper(localName = "i18nEntries")
    @JacksonXmlProperty(localName = "entry")
    private List<I18nEntry> i18nEntries = new ArrayList<>();

    @JacksonXmlElementWrapper(localName = "dataSourceRequirements")
    @JacksonXmlProperty(localName = "requirement")
    private List<DataSourceRequirement> dataSourceRequirements = new ArrayList<>();
}
