package com.archvis.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyncEvent {
    public enum Type { SYNC_STARTED, SYNC_REPO_DONE, SYNC_ALL_DONE, MODEL_REBUILT }

    private Type event;
    private String repoName;
    private String timestamp;
    private Integer totalRepos;
    private Integer failedRepos;
    private Integer featureCount;
    private Integer datasourceCount;

    public static SyncEvent started() {
        return SyncEvent.builder().event(Type.SYNC_STARTED).timestamp(Instant.now().toString()).build();
    }

    public static SyncEvent repoDone(String repoName, boolean success) {
        return SyncEvent.builder().event(Type.SYNC_REPO_DONE)
                .repoName(repoName).timestamp(Instant.now().toString()).build();
    }

    public static SyncEvent allDone(int total, int failed) {
        return SyncEvent.builder().event(Type.SYNC_ALL_DONE)
                .totalRepos(total).failedRepos(failed).timestamp(Instant.now().toString()).build();
    }

    public static SyncEvent modelRebuilt(int features, int datasources) {
        return SyncEvent.builder().event(Type.MODEL_REBUILT)
                .featureCount(features).datasourceCount(datasources)
                .timestamp(Instant.now().toString()).build();
    }
}
