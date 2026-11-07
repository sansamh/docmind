package com.docmind.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.docmind.chat.dto.ChatDtos.SourceChunk;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RagService {

    private static final int TOP_K = 5;

    private final ChatMessageRepository messageRepository;

    private static final String SYSTEM_PROMPT = """
        You are a helpful assistant that answers questions strictly based on the provided \
        context extracted from the user's document knowledge base.

        Rules:
        - Answer ONLY from the context below. Never invent facts.
        - Cite sources inline using their numbers, e.g. [1], right after the supporting statement.
        - If the context does not contain enough information to answer, say so honestly \
        and suggest what kind of document might help.
        - Answer in the same language the question is asked in.
        - Be concise and well-structured. Use markdown when helpful.
        """;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public RagService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, ObjectMapper objectMapper,
                      ChatMessageRepository messageRepository) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
        this.messageRepository = messageRepository;
    }

    /**
     * Streams an answer to the emitter:
     * - event "sources": JSON array of retrieved chunks (sent first)
     * - event "token":   one text fragment of the answer (many times)
     * - event "done":    empty, closes the stream
     * - event "error":   JSON error message, closes the stream
     */
    public void ask(Long kbId, Long userId, String question, SseEmitter emitter) {
        executor.submit(() -> {
            try {
                save(kbId, userId, ChatMessage.Role.USER, question, null);

                List<Document> hits = retrieve(kbId, question);
                List<SourceChunk> sources = toSources(hits);
                String sourcesJson = objectMapper.writeValueAsString(sources);
                emitter.send(SseEmitter.event().name("sources")
                    .data(sourcesJson));

                if (hits.isEmpty()) {
                    String message = "I couldn't find anything related to your question in this knowledge base. "
                        + "Try uploading documents that cover the topic first.";
                    emitter.send(SseEmitter.event().name("token").data(message));
                    save(kbId, userId, ChatMessage.Role.ASSISTANT, message, sourcesJson);
                    emitter.send(SseEmitter.event().name("done").data(""));
                    emitter.complete();
                    return;
                }

                String context = buildContext(hits);
                StringBuilder answer = new StringBuilder();

                chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(u -> u.text("Context:\n" + context + "\n\nQuestion: " + question))
                    .stream()
                    .content()
                    .doOnNext(token -> {
                        try {
                            answer.append(token);
                            emitter.send(SseEmitter.event().name("token").data(token));
                        } catch (Exception e) {
                            log.warn("SSE send failed", e);
                        }
                    })
                    .doOnError(emitter::completeWithError)
                    .doOnComplete(() -> {
                        try {
                            save(kbId, userId, ChatMessage.Role.ASSISTANT, answer.toString(), sourcesJson);
                            emitter.send(SseEmitter.event().name("done").data(""));
                        } catch (Exception e) {
                            log.warn("Could not finalize SSE stream", e);
                        } finally {
                            emitter.complete();
                        }
                    })
                    .blockLast();
            } catch (Exception e) {
                log.error("RAG chat failed for kb {}", kbId, e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                        .data("{\"message\":\"" + sanitize(e.getMessage()) + "\"}"));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });
    }

    private void save(Long kbId, Long userId, ChatMessage.Role role, String content, String sourcesJson) {
        try {
            messageRepository.save(ChatMessage.builder()
                .kbId(kbId)
                .userId(userId)
                .role(role)
                .content(content)
                .sourcesJson(sourcesJson)
                .build());
        } catch (Exception e) {
            log.warn("Could not persist chat message", e);
        }
    }

    private List<Document> retrieve(Long kbId, String question) {
        var filter = new FilterExpressionBuilder().eq("kbId", kbId.toString()).build();
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder().query(question).topK(TOP_K).filterExpression(filter).build());
        return docs == null ? List.of() : docs;
    }

    private List<SourceChunk> toSources(List<Document> hits) {
        List<SourceChunk> sources = new ArrayList<>();
        for (int i = 0; i < hits.size(); i++) {
            Document doc = hits.get(i);
            Double distance = doc.getMetadata().get("distance") instanceof Number n ? n.doubleValue() : null;
            Double score = distance == null ? null : Math.max(0.0, 1.0 - distance);
            sources.add(new SourceChunk(i + 1,
                String.valueOf(doc.getMetadata().getOrDefault("docName", "unknown")),
                truncate(doc.getText(), 300), score));
        }
        return sources;
    }

    private String buildContext(List<Document> hits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            Document doc = hits.get(i);
            sb.append("[").append(i + 1).append("] source: ")
                .append(doc.getMetadata().getOrDefault("docName", "unknown"))
                .append("\n").append(doc.getText()).append("\n\n");
        }
        return sb.toString();
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private String sanitize(String msg) {
        return msg == null ? "unknown error" : msg.replace("\"", "'").replace("\n", " ");
    }
}
