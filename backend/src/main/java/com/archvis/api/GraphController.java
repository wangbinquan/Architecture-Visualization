package com.archvis.api;

import com.archvis.model.ArchModelQueryService;
import com.archvis.model.GraphData;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/graph")
@RequiredArgsConstructor
public class GraphController {

    private final ArchModelQueryService queryService;

    @GetMapping("/full")
    public GraphData fullGraph() {
        return queryService.getFullGraph();
    }

    @GetMapping("/feature/{id}")
    public GraphData featureGraph(@PathVariable String id) {
        GraphData full = queryService.getFullGraph();
        // Filter to nodes/edges related to this feature
        var nodes = full.nodes().stream()
                .filter(n -> n.group() != null && (n.group().equals(id) || n.id().equals(id)
                        || n.id().startsWith(id + "/")))
                .toList();
        var nodeIds = nodes.stream().map(GraphData.GraphNode::id).collect(java.util.stream.Collectors.toSet());
        var edges = full.edges().stream()
                .filter(e -> nodeIds.contains(e.source()) || nodeIds.contains(e.target()))
                .toList();
        return new GraphData(nodes, edges);
    }
}
