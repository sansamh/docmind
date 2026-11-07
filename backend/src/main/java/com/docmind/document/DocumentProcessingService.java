package com.docmind.document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.docmind.config.AppProperties;
import com.docmind.document.parser.DocumentParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final KbDocumentRepository documentRepository;
    private final List<DocumentParser> parsers;
    private final VectorStore vectorStore;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Async
    public void process(Long docId) {
        KbDocument doc = documentRepository.findById(docId).orElse(null);
        if (doc == null) {
            return;
        }
        try {
            Path file = filePath(doc);
            String text = parserFor(doc.getFileName()).extractText(file);
            if (text == null || text.isBlank()) {
                fail(doc, "No extractable text found in file");
                return;
            }
            TokenTextSplitter splitter = new TokenTextSplitter();
            Document source = Document.builder()
                .text(text)
                .metadata(Map.of(
                    "kbId", doc.getKbId().toString(),
                    "docId", doc.getId().toString(),
                    "docName", doc.getFileName()))
                .build();
            List<Document> chunks = splitter.apply(List.of(source));

            vectorStore.add(chunks);
            persistVectorStore();

            doc.setStatus(KbDocument.Status.READY);
            doc.setChunkCount(chunks.size());
            doc.setChunkIdsJson(objectMapper.writeValueAsString(
                chunks.stream().map(Document::getId).toList()));
            documentRepository.save(doc);
            log.info("Document {} indexed into {} chunks", docId, chunks.size());
        } catch (Exception e) {
            log.error("Failed to process document {}", docId, e);
            fail(doc, rootMessage(e));
        }
    }

    public void deleteVectors(KbDocument doc) {
        if (doc.getChunkIdsJson() == null) {
            return;
        }
        try {
            List<String> ids = objectMapper.readValue(doc.getChunkIdsJson(), new TypeReference<List<String>>() {});
            if (!ids.isEmpty()) {
                vectorStore.delete(ids);
                persistVectorStore();
            }
        } catch (Exception e) {
            log.warn("Could not delete vectors for document {}", doc.getId(), e);
        }
    }

    private void persistVectorStore() {
        if (vectorStore instanceof SimpleVectorStore simple) {
            try {
                simple.save(new java.io.File(appProperties.getVectorStorePath()));
            } catch (Exception e) {
                log.warn("Could not persist vector store", e);
            }
        }
    }

    private DocumentParser parserFor(String fileName) {
        return parsers.stream()
            .filter(p -> p.supports(fileName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unsupported file type. Supported: pdf, docx, txt, md, csv, json"));
    }

    private Path filePath(KbDocument doc) {
        return Path.of(appProperties.getUploadDir(), doc.getId() + "_" + doc.getFileName());
    }

    private void fail(KbDocument doc, String message) {
        doc.setStatus(KbDocument.Status.FAILED);
        doc.setErrorMessage(message);
        documentRepository.save(doc);
    }

    private String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        String msg = cur.getMessage();
        return msg != null && msg.length() <= 500 ? msg : "Processing failed";
    }
}
