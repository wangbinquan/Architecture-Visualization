package com.archvis.model;

import com.archvis.domain.DataSourceDef;
import com.archvis.domain.DependencyLink;
import com.archvis.domain.Feature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArchModelQueryService {

    private final InMemoryArchModel model;

    public List<Feature> getAllFeatures() {
        return model.get().getFeatures();
    }

    public Optional<Feature> getFeatureById(String id) {
        return model.get().getFeatures().stream().filter(f -> id.equals(f.getId())).findFirst();
    }

    public List<DataSourceDef> getAllDataSources() {
        return model.get().getDataSources();
    }

    public Optional<DataSourceDef> getDataSourceById(String id) {
        return model.get().getDataSources().stream().filter(d -> id.equals(d.getId())).findFirst();
    }

    public List<DependencyLink> getDependenciesForFeature(String featureId) {
        return model.get().getDependencyLinks().stream()
                .filter(l -> featureId.equals(l.getFeatureId()))
                .toList();
    }

    public List<DependencyLink> getReferencesForDataSource(String datasourceId) {
        return model.get().getDependencyLinks().stream()
                .filter(l -> datasourceId.equals(l.getDatasourceId()))
                .toList();
    }

    public GraphData getFullGraph() {
        ModelSnapshot snapshot = model.get();
        List<GraphData.GraphNode> nodes = new ArrayList<>();
        List<GraphData.GraphEdge> edges = new ArrayList<>();

        for (Feature f : snapshot.getFeatures()) {
            nodes.add(new GraphData.GraphNode(f.getId(), f.getName(), "feature", f.getSourceRepo()));
            if (f.getConfigs() != null) {
                for (var cfg : f.getConfigs()) {
                    String cfgNodeId = f.getId() + "/" + cfg.getId();
                    nodes.add(new GraphData.GraphNode(cfgNodeId, cfg.getName(), "feature-config", f.getId()));
                    edges.add(new GraphData.GraphEdge(
                            f.getId() + "->" + cfgNodeId, f.getId(), cfgNodeId, "contains", "contains"));
                }
            }
        }

        for (DataSourceDef ds : snapshot.getDataSources()) {
            nodes.add(new GraphData.GraphNode(ds.getId(), ds.getName(), "datasource", ds.getSourceRepo()));
            if (ds.getSubCollections() != null) {
                for (var col : ds.getSubCollections()) {
                    String colId = ds.getId() + "/" + col.getId();
                    nodes.add(new GraphData.GraphNode(colId, col.getName(), "collection", ds.getId()));
                    edges.add(new GraphData.GraphEdge(
                            ds.getId() + "->" + colId, ds.getId(), colId, "contains", "contains"));
                    if (col.getBusinessMetrics() != null) {
                        for (var metric : col.getBusinessMetrics()) {
                            String metricId = colId + "/" + metric.getId();
                            nodes.add(new GraphData.GraphNode(metricId, metric.getName(), "metric", colId));
                            edges.add(new GraphData.GraphEdge(
                                    colId + "->" + metricId, colId, metricId, "contains", "contains"));
                        }
                    }
                }
            }
        }

        for (DependencyLink link : snapshot.getDependencyLinks()) {
            String configNodeId = link.getFeatureId() + "/" + link.getFeatureConfigId();
            String metricNodeId = link.getDatasourceId() + "/" + link.getCollectionId() + "/" + link.getMetricId();
            String edgeId = configNodeId + "=>" + metricNodeId;
            edges.add(new GraphData.GraphEdge(edgeId, configNodeId, metricNodeId, "references", "uses"));
        }

        return new GraphData(nodes, edges);
    }
}
