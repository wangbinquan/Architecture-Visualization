package com.archvis.sync;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SyncResult {
    private String repoName;
    private boolean success;
    private String message;
    private Instant syncedAt;

    public static SyncResult ok(String repoName) {
        return SyncResult.builder()
                .repoName(repoName)
                .success(true)
                .message("OK")
                .syncedAt(Instant.now())
                .build();
    }

    public static SyncResult fail(String repoName, String error) {
        return SyncResult.builder()
                .repoName(repoName)
                .success(false)
                .message(error)
                .syncedAt(Instant.now())
                .build();
    }
}
