package com.archvis.editor;

import com.archvis.sync.GitCommandExecutor;
import com.archvis.sync.RepoSyncService;
import com.archvis.sync.ReposConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class MrService {

    private final GitCommandExecutor git;
    private final RepoSyncService syncService;
    private final ReposConfigProperties config;

    /**
     * Applies file changes to the repo, creates a branch, commits, pushes, and returns branch info.
     * MR creation against the git platform is deferred to business phase (B-07).
     */
    public EditResult submitEdit(EditRequest request, String repoName, Path targetFile, String newContent) {
        Path repoPath = syncService.localPathFor(repoName);
        String branch = "arch-vis/edit-" + Instant.now().toEpochMilli()
                + "-" + sanitize(request.getEntityId());

        try {
            // 1. Create branch
            GitCommandExecutor.ProcessResult branchResult = git.checkoutBranch(repoPath, branch);
            if (!branchResult.success()) {
                return EditResult.fail("Failed to create branch: " + branchResult.output());
            }

            // 2. Write file change
            Files.writeString(targetFile, newContent);

            // 3. Commit
            String relPath = repoPath.relativize(targetFile).toString();
            git.add(repoPath, relPath);
            String commitMsg = "[arch-vis] " + request.getEntityType() + "/" + request.getEntityId()
                    + ": update " + request.getField();
            GitCommandExecutor.ProcessResult commitResult = git.commit(repoPath, commitMsg);
            if (!commitResult.success()) {
                return EditResult.fail("Commit failed: " + commitResult.output());
            }

            // 4. Push
            GitCommandExecutor.ProcessResult pushResult = git.push(repoPath, branch);
            if (!pushResult.success()) {
                return EditResult.fail("Push failed: " + pushResult.output());
            }

            String commitHash = commitResult.output().lines()
                    .filter(l -> l.startsWith("["))
                    .findFirst()
                    .orElse("unknown");

            return EditResult.ok(branch, commitHash);

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return EditResult.fail(e.getMessage());
        }
    }

    private String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9-]", "-").toLowerCase();
    }
}
