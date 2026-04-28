package com.archvis.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class DataSourceDef {
    private String id;
    private String name;
    private String sourceRepo;
    private List<SubCollection> subCollections = new ArrayList<>();
}
