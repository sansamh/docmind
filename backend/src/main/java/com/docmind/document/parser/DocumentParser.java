package com.docmind.document.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface DocumentParser {

    List<String> supportedExtensions();

    String extractText(Path file) throws IOException;

    default boolean supports(String fileName) {
        String name = fileName.toLowerCase();
        return supportedExtensions().stream().anyMatch(name::endsWith);
    }
}
