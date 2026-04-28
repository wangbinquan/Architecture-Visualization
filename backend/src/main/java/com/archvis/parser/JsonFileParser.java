package com.archvis.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class JsonFileParser implements FileParser<Object> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> T parse(Path file, Class<T> type) throws Exception {
        return objectMapper.readValue(file.toFile(), type);
    }

    @Override
    public Object parse(Path file) throws Exception {
        return objectMapper.readValue(file.toFile(), Object.class);
    }

    @Override
    public boolean supports(Path file) {
        return file.toString().endsWith(".json");
    }
}
