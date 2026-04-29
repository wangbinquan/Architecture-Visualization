package com.archvis.mcp.tool;

import com.archvis.editor.EditRequest;
import com.archvis.editor.EditResult;
import com.archvis.editor.MrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EditorTools {

    private final MrService mrService;

    // MR history kept in memory; persistent storage deferred to business phase (B-07).
    // Parallel to EditorController.mrHistory — both paths log to their own list until refactor.
    private final List<EditResult> mrHistory = new ArrayList<>();

    @Tool(name = "preview_edit",
            description = "Preview the effect of editing a single field on a feature/datasource/feature-config/metric. "
                    + "Returns a placeholder description of the change without touching files. "
                    + "Schema-mapping (B-01) is required before real diffs can be computed.")
    public String previewEdit(
            @ToolParam(description = "Type of entity: feature | datasource | feature-config | metric") String entityType,
            @ToolParam(description = "Id of the entity to edit") String entityId,
            @ToolParam(description = "Field on the entity to update") String field,
            @ToolParam(description = "Current value (optional, used in the preview text)", required = false) String oldValue,
            @ToolParam(description = "New value to set") String newValue) {
        return "Would update " + entityType + "/" + entityId
                + " field '" + field + "' from '" + oldValue + "' to '" + newValue + "'";
    }

    @Tool(name = "submit_edit",
            description = "Submit a single-field edit. In the current build this is a stub that returns failure "
                    + "with a B-01 schema-mapping note — the underlying MrService can create branch+commit+push "
                    + "but file-change generation is not yet wired. The result is also recorded in the MR history.")
    public EditResult submitEdit(
            @ToolParam(description = "Type of entity: feature | datasource | feature-config | metric") String entityType,
            @ToolParam(description = "Id of the entity to edit") String entityId,
            @ToolParam(description = "Field on the entity to update") String field,
            @ToolParam(description = "Current value (optional)", required = false) String oldValue,
            @ToolParam(description = "New value to set") String newValue,
            @ToolParam(description = "Free-text rationale for the edit (optional)", required = false) String description,
            @ToolParam(description = "If true, route through configured AI tool adapter for complex changes (currently no-op)", required = false) Boolean useAiTool) {
        EditRequest request = new EditRequest();
        request.setEntityType(entityType);
        request.setEntityId(entityId);
        request.setField(field);
        request.setOldValue(oldValue);
        request.setNewValue(newValue);
        request.setDescription(description);
        request.setUseAiTool(Boolean.TRUE.equals(useAiTool));

        log.info("MCP submit_edit stub: {}/{} {} -> {}", entityType, entityId, field, newValue);
        EditResult result = EditResult.fail(
                "Edit submission requires schema mapping (B-01). Stub response only.");
        mrHistory.add(result);
        return result;
    }

    @Tool(name = "list_recent_mrs",
            description = "List the in-memory MR history recorded by the MCP editor tools in this server session.")
    public List<EditResult> listRecentMrs() {
        return List.copyOf(mrHistory);
    }
}
