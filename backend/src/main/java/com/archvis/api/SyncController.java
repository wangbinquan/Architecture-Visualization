package com.archvis.api;

import com.archvis.model.ModelRebuildService;
import com.archvis.model.ModelSnapshot;
import com.archvis.sync.RepoSyncService;
import com.archvis.sync.ReposConfigProperties;
import com.archvis.sync.SyncResult;
import com.archvis.sync.SyncScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final RepoSyncService syncService;
    private final SyncScheduler scheduler;
    private final ModelRebuildService modelRebuildService;
    private final ReposConfigProperties config;

    @GetMapping("/status")
    public Map<String, Object> status() {
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

    @PostMapping("/trigger")
    public Map<String, Object> triggerAll() {
        List<SyncResult> results = syncService.syncAll();
        ModelSnapshot snapshot = modelRebuildService.rebuildModel();
        long ok = results.stream().filter(SyncResult::isSuccess).count();
        return Map.of("total", results.size(), "success", ok, "failed", results.size() - ok,
                "featuresLoaded", snapshot.getFeatures().size(),
                "datasourcesLoaded", snapshot.getDataSources().size());
    }

    @PostMapping("/trigger/{repoName}")
    public SyncResult triggerOne(@PathVariable String repoName) {
        return config.getRepositories().stream()
                .filter(r -> r.getName().equals(repoName))
                .findFirst()
                .map(r -> {
                    SyncResult result = syncService.syncOne(r);
                    modelRebuildService.rebuildModel();
                    return result;
                })
                .orElse(SyncResult.fail(repoName, "Repo not found in config"));
    }
}
