package com.archvis.sync;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RepoEntry {
    private String name;
    private String url;
    private String branch = "main";
}
