package com.archvis.api;

import com.archvis.domain.DependencyLink;
import com.archvis.domain.Feature;
import com.archvis.model.ArchModelQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/features")
@RequiredArgsConstructor
public class FeatureController {

    private final ArchModelQueryService queryService;

    @GetMapping
    public List<Feature> list(@RequestParam(required = false) String search) {
        List<Feature> all = queryService.getAllFeatures();
        if (search == null || search.isBlank()) return all;
        String q = search.toLowerCase();
        return all.stream()
                .filter(f -> f.getName().toLowerCase().contains(q) || f.getId().toLowerCase().contains(q))
                .toList();
    }

    @GetMapping("/{id}")
    public Feature get(@PathVariable String id) {
        return queryService.getFeatureById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feature not found: " + id));
    }

    @GetMapping("/{id}/dependencies")
    public List<DependencyLink> dependencies(@PathVariable String id) {
        return queryService.getDependenciesForFeature(id);
    }
}
