package com.archvis.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

@Component
@RequiredArgsConstructor
public class ParserFactory {

    private final XmlFileParser xmlParser;
    private final JsonFileParser jsonParser;
    private final PropertiesFileParser propertiesParser;

    public <T> Optional<T> parseXml(Path file, Class<T> type) {
        try {
            return Optional.of(xmlParser.parse(file, type));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public <T> Optional<T> parseJson(Path file, Class<T> type) {
        try {
            return Optional.of(jsonParser.parse(file, type));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Properties> parseProperties(Path file) {
        try {
            return Optional.of(propertiesParser.parse(file));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
