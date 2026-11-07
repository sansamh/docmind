package com.docmind.chat;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.docmind.auth.AuthUser;
import com.docmind.chat.dto.ChatDtos.ChatRequest;
import com.docmind.chat.dto.ChatDtos.MessageResponse;
import com.docmind.chat.dto.ChatDtos.SourceChunk;
import com.docmind.kb.KnowledgeBase;
import com.docmind.kb.KnowledgeBaseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/kb/{kbId}/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private static final long SSE_TIMEOUT = 120_000L;

    private final RagService ragService;
    private final ChatMessageRepository messageRepository;
    private final KnowledgeBaseRepository kbRepository;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<SseEmitter> chat(@AuthenticationPrincipal AuthUser user,
                                           @PathVariable Long kbId,
                                           @Valid @RequestBody ChatRequest req) {
        if (ownedKb(user, kbId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        ragService.ask(kbId, user.userId(), req.question(), emitter);
        return ResponseEntity.ok(emitter);
    }

    @GetMapping("/messages")
    public ResponseEntity<List<MessageResponse>> history(@AuthenticationPrincipal AuthUser user,
                                                         @PathVariable Long kbId) {
        if (ownedKb(user, kbId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<MessageResponse> messages = messageRepository
            .findByKbIdAndUserIdOrderByIdAsc(kbId, user.userId()).stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(messages);
    }

    private Optional<KnowledgeBase> ownedKb(AuthUser user, Long kbId) {
        return kbRepository.findById(kbId)
            .filter(kb -> kb.getOwner().getId().equals(user.userId()));
    }

    private MessageResponse toResponse(ChatMessage msg) {
        List<SourceChunk> sources = List.of();
        if (msg.getSourcesJson() != null) {
            try {
                sources = objectMapper.readValue(msg.getSourcesJson(), new TypeReference<List<SourceChunk>>() {});
            } catch (Exception e) {
                log.warn("Could not parse sources JSON for message {}", msg.getId());
            }
        }
        return new MessageResponse(msg.getId(), msg.getRole().name().toLowerCase(),
            msg.getContent(), sources, msg.getCreatedAt().toString());
    }
}
