package com.archvis.parser;

import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Properties;

@Component
public class PropertiesFileParser implements FileParser<Properties> {

    @Override
    public Properties parse(Path file) throws Exception {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            props.load(fis);
        }
        return props;
    }

    @Override
    public boolean supports(Path file) {
        return file.toString().endsWith(".properties");
    }
}
