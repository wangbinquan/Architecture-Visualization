package com.archvis.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "archvis.mcp")
public class McpProperties {

    private boolean enabled = true;
}
