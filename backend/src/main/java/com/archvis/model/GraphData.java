package com.archvis.model;

import java.util.List;

public record GraphData(List<GraphNode> nodes, List<GraphEdge> edges) {
    public record GraphNode(String id, String label, String type, String group) {}
    public record GraphEdge(String id, String source, String target, String type, String label) {}
}
