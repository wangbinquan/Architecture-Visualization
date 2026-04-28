package com.archvis.model;

import com.archvis.sync.ReposSyncCompletedEvent;
import com.archvis.sync.ReposConfigProperties;
import com.archvis.sync.RepoSyncService;
import com.archvis.websocket.SyncEvent;
import com.archvis.websocket.SyncStatusWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ModelRebuildService {

    private final ReposConfigProperties config;
    private final RepoSyncService syncService;
    private final RepoModelAssembler assembler;
    private final InMemoryArchModel model;
    private final SyncStatusWebSocketHandler wsHandler;

    @EventListener
    public void onSyncCompleted(ReposSyncCompletedEvent event) {
        log.info("Sync completed, rebuilding in-memory model...");
        rebuildModel();
    }

    public ModelSnapshot rebuildModel() {
        List<RepoModelAssembler.RepoLocalPath> paths = config.getRepositories().stream()
                .map(r -> new RepoModelAssembler.RepoLocalPath(r.getName(), syncService.localPathFor(r.getName())))
                .toList();

        ModelSnapshot snapshot = assembler.assemble(paths);
        model.update(snapshot);
        wsHandler.broadcast(SyncEvent.modelRebuilt(
                snapshot.getFeatures().size(),
                snapshot.getDataSources().size()
        ));
        return snapshot;
    }
}
