package com.archvis.sync;

import com.archvis.websocket.SyncEvent;
import com.archvis.websocket.SyncStatusWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class SyncScheduler {

    private final RepoSyncService syncService;
    private final ApplicationEventPublisher eventPublisher;
    private final SyncStatusWebSocketHandler wsHandler;

    @Scheduled(fixedDelayString = "#{${archvis.sync.interval-seconds:300} * 1000}",
               initialDelayString = "5000")
    public void syncAll() {
        log.info("Starting scheduled repo sync");
        wsHandler.broadcast(SyncEvent.started());

        List<SyncResult> results = syncService.syncAll();

        long failed = results.stream().filter(r -> !r.isSuccess()).count();
        log.info("Sync complete: {}/{} repos succeeded", results.size() - failed, results.size());

        wsHandler.broadcast(SyncEvent.allDone(results.size(), (int) failed));
        eventPublisher.publishEvent(new ReposSyncCompletedEvent(this, results));
    }

    /** Manual trigger endpoint support */
    public void triggerNow() {
        syncAll();
    }
}
