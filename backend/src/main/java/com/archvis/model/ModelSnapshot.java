package com.archvis.model;

import com.archvis.domain.DataSourceDef;
import com.archvis.domain.DependencyLink;
import com.archvis.domain.Feature;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
public class ModelSnapshot {
    List<Feature> features;
    List<DataSourceDef> dataSources;
    List<DependencyLink> dependencyLinks;
    Instant builtAt;

    public static ModelSnapshot empty() {
        return new ModelSnapshot(List.of(), List.of(), List.of(), Instant.now());
    }

    public static ModelSnapshot of(List<Feature> features, List<DataSourceDef> dataSources,
                                   List<DependencyLink> links) {
        return new ModelSnapshot(features, dataSources, links, Instant.now());
    }
}
