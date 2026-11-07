package com.docmind.document.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

@Component
public class DocxTextParser implements DocumentParser {

    @Override
    public List<String> supportedExtensions() {
        return List.of(".docx");
    }

    @Override
    public String extractText(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file);
             XWPFDocument doc = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
