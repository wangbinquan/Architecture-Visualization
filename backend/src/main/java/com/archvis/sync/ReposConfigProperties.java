package com.archvis.sync;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "archvis")
public class ReposConfigProperties {

    private SyncConfig sync = new SyncConfig();
    private List<RepoEntry> repositories = new ArrayList<>();
    private AiToolsConfig aiTools = new AiToolsConfig();

    @Data
    public static class SyncConfig {
        private int intervalSeconds = 300;
        private String localBasePath = System.getProperty("user.home") + "/.archvis/repos";
    }

    @Data
    public static class AiToolsConfig {
        private boolean enabled = false;
        private String defaultTool = "claude-code";
        private String claudeCodeCliPath = "/usr/local/bin/claude";
        private String opencodeCliPath = "/usr/local/bin/opencode";
    }
}
