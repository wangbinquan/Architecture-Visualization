package com.archvis.mcp;

import com.archvis.mcp.tool.ArchitectureQueryTools;
import com.archvis.mcp.tool.EditorTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "archvis.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider archVisToolCallbackProvider(
            ArchitectureQueryTools queryTools,
            EditorTools editorTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(queryTools, editorTools)
                .build();
    }
}
