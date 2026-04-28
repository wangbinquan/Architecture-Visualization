package com.archvis.parser;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class XmlFileParser implements FileParser<Object> {

    private final XmlMapper xmlMapper = new XmlMapper();

    public <T> T parse(Path file, Class<T> type) throws Exception {
        return xmlMapper.readValue(file.toFile(), type);
    }

    @Override
    public Object parse(Path file) throws Exception {
        return xmlMapper.readValue(file.toFile(), Object.class);
    }

    @Override
    public boolean supports(Path file) {
        return file.toString().endsWith(".xml");
    }
}
