package com.archvis.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DependencyLink {
    private String featureId;
    private String featureConfigId;
    private String datasourceId;
    private String collectionId;
    private String metricId;

    public String getTargetPath() {
        return datasourceId + "/" + collectionId + "/" + metricId;
    }
}
