package com.archvis.editor;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EditRequest {
    @NotBlank
    private String entityType;   // "feature" | "datasource" | "feature-config" | "metric"
    @NotBlank
    private String entityId;
    @NotBlank
    private String field;
    private String oldValue;
    @NotBlank
    private String newValue;
    private String description;
    private boolean useAiTool = false;
}
