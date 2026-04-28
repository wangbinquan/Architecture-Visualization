package com.archvis.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BusinessMetric {
    private String id;
    private String name;
    private String dataType;
    private String description;
}
