package com.docmind.document.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class PlainTextParser implements DocumentParser {

    @Override
    public List<String> supportedExtensions() {
        return List.of(".txt", ".md", ".markdown", ".csv", ".json");
    }

    @Override
    public String extractText(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
