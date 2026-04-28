package com.archvis.sync;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class ReposSyncCompletedEvent extends ApplicationEvent {

    private final List<SyncResult> results;

    public ReposSyncCompletedEvent(Object source, List<SyncResult> results) {
        super(source);
        this.results = results;
    }

    public List<SyncResult> getResults() {
        return results;
    }
}
