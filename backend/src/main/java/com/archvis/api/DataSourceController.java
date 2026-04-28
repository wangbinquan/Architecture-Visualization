package com.archvis.api;

import com.archvis.domain.DataSourceDef;
import com.archvis.domain.DependencyLink;
import com.archvis.model.ArchModelQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final ArchModelQueryService queryService;

    @GetMapping
    public List<DataSourceDef> list(@RequestParam(required = false) String search) {
        List<DataSourceDef> all = queryService.getAllDataSources();
        if (search == null || search.isBlank()) return all;
        String q = search.toLowerCase();
        return all.stream()
                .filter(d -> d.getName().toLowerCase().contains(q) || d.getId().toLowerCase().contains(q))
                .toList();
    }

    @GetMapping("/{id}")
    public DataSourceDef get(@PathVariable String id) {
        return queryService.getDataSourceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DataSource not found: " + id));
    }

    @GetMapping("/{id}/referenced-by")
    public List<DependencyLink> referencedBy(@PathVariable String id) {
        return queryService.getReferencesForDataSource(id);
    }
}
