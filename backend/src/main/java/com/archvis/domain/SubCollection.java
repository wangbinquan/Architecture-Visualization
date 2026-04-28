package com.archvis.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class SubCollection {
    private String id;
    private String name;
    private List<BusinessMetric> businessMetrics = new ArrayList<>();
}
