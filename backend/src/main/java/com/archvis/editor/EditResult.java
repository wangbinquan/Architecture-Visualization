package com.archvis.editor;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EditResult {
    private boolean success;
    private String branchName;
    private String commitHash;
    private String mrUrl;
    private String message;

    public static EditResult ok(String branch, String commit) {
        return EditResult.builder().success(true).branchName(branch).commitHash(commit)
                .message("Branch pushed. Please create MR manually or via git platform.").build();
    }

    public static EditResult fail(String reason) {
        return EditResult.builder().success(false).message(reason).build();
    }
}
