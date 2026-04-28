package com.archvis.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class RepoSyncService {

    private final ReposConfigProperties config;
    private final GitCommandExecutor git;
    private final Map<String, SyncResult> lastResults = new ConcurrentHashMap<>();

    public List<SyncResult> syncAll() {
        List<RepoEntry> repos = config.getRepositories();
        if (repos.isEmpty()) {
            log.warn("No repositories configured in archvis.repositories");
            return List.of();
        }

        List<SyncResult> results = new ArrayList<>();
        for (RepoEntry repo : repos) {
            SyncResult result = syncOne(repo);
            results.add(result);
            lastResults.put(repo.getName(), result);
        }
        return results;
    }

    public SyncResult syncOne(RepoEntry repo) {
        Path localPath = localPathFor(repo.getName());
        try {
            if (Files.exists(localPath.resolve(".git"))) {
                pull(repo, localPath);
            } else {
                Files.createDirectories(localPath.getParent());
                GitCommandExecutor.ProcessResult result = git.clone(repo.getUrl(), repo.getBranch(), localPath);
                if (!result.success()) {
                    return SyncResult.fail(repo.getName(), result.output());
                }
            }
            log.info("Synced repo: {}", repo.getName());
            return SyncResult.ok(repo.getName());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return SyncResult.fail(repo.getName(), e.getMessage());
        }
    }

    private void pull(RepoEntry repo, Path localPath) throws IOException, InterruptedException {
        git.fetch(localPath);
        git.resetHard(localPath, repo.getBranch());
    }

    public Path localPathFor(String repoName) {
        return Paths.get(config.getSync().getLocalBasePath(), repoName);
    }

    public Map<String, SyncResult> getLastResults() {
        return Map.copyOf(lastResults);
    }
}
