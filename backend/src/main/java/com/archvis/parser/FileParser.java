package com.archvis.parser;

import java.nio.file.Path;

public interface FileParser<T> {
    T parse(Path file) throws Exception;
    boolean supports(Path file);
}
