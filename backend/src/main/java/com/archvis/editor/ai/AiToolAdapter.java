package com.archvis.editor.ai;

import java.nio.file.Path;

public interface AiToolAdapter {

    record EditContext(Path filePath, String currentContent, String changeDescription) {}
    record FileChange(String newContent, String explanation) {}

    FileChange generateChange(EditContext context) throws Exception;

    String toolName();
}
