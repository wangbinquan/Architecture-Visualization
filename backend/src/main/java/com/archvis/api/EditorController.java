package com.archvis.api;

import com.archvis.editor.EditRequest;
import com.archvis.editor.EditResult;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/editor")
@Slf4j
public class EditorController {

    // MR history kept in memory (session-scoped); persistent storage deferred to business phase
    private final List<EditResult> mrHistory = new ArrayList<>();

    @PostMapping("/preview")
    public String preview(@Valid @RequestBody EditRequest request) {
        // Stub: return a placeholder diff description
        return "Would update " + request.getEntityType() + "/" + request.getEntityId()
                + " field '" + request.getField() + "' from '" + request.getOldValue()
                + "' to '" + request.getNewValue() + "'";
    }

    @PostMapping("/submit")
    public EditResult submit(@Valid @RequestBody EditRequest request) {
        // Full file-change + git flow deferred to business phase (B-01/B-02 schema mapping required first)
        log.info("Edit submit stub: {}/{} {} -> {}", request.getEntityType(), request.getEntityId(),
                request.getField(), request.getNewValue());
        EditResult result = EditResult.fail(
                "Edit submission requires schema mapping (B-01). Stub response only.");
        mrHistory.add(result);
        return result;
    }

    @GetMapping("/mrs")
    public List<EditResult> mrHistory() {
        return List.copyOf(mrHistory);
    }
}
