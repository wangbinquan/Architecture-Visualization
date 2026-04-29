package com.archvis.mcp.tool;

import com.archvis.domain.DataSourceDef;
import com.archvis.domain.DependencyLink;
import com.archvis.domain.Feature;
import com.archvis.model.ArchModelQueryService;
import com.archvis.model.GraphData;
import com.archvis.sync.RepoSyncService;
import com.archvis.sync.ReposConfigProperties;
import com.archvis.sync.SyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArchitectureQueryTools {

    private final ArchModelQueryService queryService;
    private final RepoSyncService syncService;
    private final ReposConfigProperties config;

    @Tool(name = "list_features",
            description = "List all features in the architecture model. Optionally filter by a case-insensitive substring match on name or id.")
    public List<Feature> listFeatures(
            @ToolParam(description = "Optional case-insensitive substring filter on feature name or id", required = false) String search) {
        List<Feature> all = queryService.getAllFeatures();
        if (search == null || search.isBlank()) {
            return all;
        }
        String needle = search.toLowerCase(Locale.ROOT);
        return all.stream()
                .filter(f -> contains(f.getName(), needle) || contains(f.getId(), needle))
                .toList();
    }

    @Tool(name = "get_feature",
            description = "Get a single feature by its id, including its configs, database tables, i18n entries, and datasource requirements. Throws if not found.")
    public Feature getFeature(
            @ToolParam(description = "Feature id") String id) {
        return queryService.getFeatureById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feature not found: " + id));
    }

    @Tool(name = "get_feature_dependencies",
            description = "Get all dependency links from a feature's configs to specific business metrics in datasources.")
    public List<DependencyLink> getFeatureDependencies(
            @ToolParam(description = "Feature id") String id) {
        return queryService.getDependenciesForFeature(id);
    }

    @Tool(name = "list_datasources",
            description = "List all datasources in the architecture model. Optionally filter by a case-insensitive substring on name or id.")
    public List<DataSourceDef> listDatasources(
            @ToolParam(description = "Optional case-insensitive substring filter on datasource name or id", required = false) String search) {
        List<DataSourceDef> all = queryService.getAllDataSources();
        if (search == null || search.isBlank()) {
            return all;
        }
        String needle = search.toLowerCase(Locale.ROOT);
        return all.stream()
                .filter(d -> contains(d.getName(), needle) || contains(d.getId(), needle))
                .toList();
    }

    @Tool(name = "get_datasource",
            description = "Get a single datasource by its id, including its sub-collections and business metrics. Throws if not found.")
    public DataSourceDef getDatasource(
            @ToolParam(description = "Datasource id") String id) {
        return queryService.getDataSourceById(id)
                .orElseThrow(() -> new IllegalArgumentException("Datasource not found: " + id));
    }

    @Tool(name = "get_datasource_referenced_by",
            description = "List the dependency links pointing into this datasource — i.e. which feature configs use any of its metrics.")
    public List<DependencyLink> getDatasourceReferencedBy(
            @ToolParam(description = "Datasource id") String id) {
        return queryService.getReferencesForDataSource(id);
    }

    @Tool(name = "get_full_graph",
            description = "Return the full architecture dependency graph: all features, configs, datasources, sub-collections, business metrics, and edges between them.")
    public GraphData getFullGraph() {
        return queryService.getFullGraph();
    }

    @Tool(name = "get_feature_graph",
            description = "Return the dependency subgraph centered on a specific feature: nodes related to the feature and edges that touch them.")
    public GraphData getFeatureGraph(
            @ToolParam(description = "Feature id") String id) {
        GraphData full = queryService.getFullGraph();
        var nodes = full.nodes().stream()
                .filter(n -> n.group() != null && (n.group().equals(id) || n.id().equals(id)
                        || n.id().startsWith(id + "/")))
                .toList();
        Set<String> nodeIds = nodes.stream().map(GraphData.GraphNode::id).collect(Collectors.toSet());
        var edges = full.edges().stream()
                .filter(e -> nodeIds.contains(e.source()) || nodeIds.contains(e.target()))
                .toList();
        return new GraphData(nodes, edges);
    }

    @Tool(name = "get_sync_status",
            description = "Return the per-repository sync status: configured repos with branch and last sync result, plus current server timestamp.")
    public Map<String, Object> getSyncStatus() {
        return Map.of(
                "repos", config.getRepositories().stream()
                        .map(r -> Map.of(
                                "name", r.getName(),
                                "branch", r.getBranch(),
                                "lastSync", syncService.getLastResults().getOrDefault(r.getName(),
                                        SyncResult.fail(r.getName(), "not synced yet"))
                        )).toList(),
                "timestamp", Instant.now()
        );
    }

    private static boolean contains(String haystack, String needleLower) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needleLower);
    }
}
