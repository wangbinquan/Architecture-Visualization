package com.archvis.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class GitCommandExecutor {

    private static final int TIMEOUT_SECONDS = 120;

    public record ProcessResult(int exitCode, String output) {
        public boolean success() { return exitCode == 0; }
    }

    public ProcessResult execute(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            return new ProcessResult(-1, "Git command timed out after " + TIMEOUT_SECONDS + "s");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.warn("Git command failed (exit {}): {}", exitCode, output.trim());
        }
        return new ProcessResult(exitCode, output);
    }

    public ProcessResult clone(String url, String branch, Path localPath) throws IOException, InterruptedException {
        log.info("Cloning {} (branch: {}) → {}", url, branch, localPath);
        return execute("git", "clone", "--branch", branch, url, localPath.toString());
    }

    public ProcessResult fetch(Path localPath) throws IOException, InterruptedException {
        return execute("git", "-C", localPath.toString(), "fetch", "origin");
    }

    public ProcessResult resetHard(Path localPath, String branch) throws IOException, InterruptedException {
        return execute("git", "-C", localPath.toString(), "reset", "--hard", "origin/" + branch);
    }

    public ProcessResult checkoutBranch(Path localPath, String branchName) throws IOException, InterruptedException {
        return execute("git", "-C", localPath.toString(), "checkout", "-b", branchName);
    }

    public ProcessResult add(Path localPath, String... files) throws IOException, InterruptedException {
        String[] cmd = new String[3 + files.length];
        cmd[0] = "git"; cmd[1] = "-C"; cmd[2] = localPath.toString();
        cmd[3] = "add";
        // actually need: git -C path add file1 file2 ...
        // rebuild properly
        String[] fullCmd = new String[4 + files.length];
        fullCmd[0] = "git"; fullCmd[1] = "-C"; fullCmd[2] = localPath.toString(); fullCmd[3] = "add";
        System.arraycopy(files, 0, fullCmd, 4, files.length);
        return execute(fullCmd);
    }

    public ProcessResult commit(Path localPath, String message) throws IOException, InterruptedException {
        return execute("git", "-C", localPath.toString(), "commit", "-m", message);
    }

    public ProcessResult push(Path localPath, String branchName) throws IOException, InterruptedException {
        return execute("git", "-C", localPath.toString(), "push", "origin", branchName);
    }
}
