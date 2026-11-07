package com.docmind.config;

import java.io.File;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dev profile: file-backed in-process vector store (zero infrastructure).
 * In the "prod" profile the PgVectorStore auto-configuration (Postgres + pgvector)
 * takes over - business code only depends on the VectorStore interface.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class VectorStoreConfig {

    private final AppProperties appProperties;

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File file = new File(appProperties.getVectorStorePath());
        if (file.exists()) {
            try {
                store.load(file);
                log.info("Loaded vector store from {}", file.getAbsolutePath());
            } catch (Exception e) {
                log.warn("Could not load existing vector store, starting empty", e);
            }
        }
        return store;
    }
}
