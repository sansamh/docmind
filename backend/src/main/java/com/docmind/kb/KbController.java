package com.docmind.kb;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docmind.auth.AuthUser;
import com.docmind.auth.UserRepository;
import com.docmind.document.KbDocumentRepository;
import com.docmind.kb.dto.KbDtos.KbRequest;
import com.docmind.kb.dto.KbDtos.KbResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
public class KbController {

    private final KnowledgeBaseRepository kbRepository;
    private final KbDocumentRepository documentRepository;
    private final UserRepository userRepository;

    @GetMapping
    public List<KbResponse> list(@AuthenticationPrincipal AuthUser user) {
        return kbRepository.findByOwnerIdOrderByCreatedAtDesc(user.userId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @PostMapping
    public KbResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody KbRequest req) {
        KnowledgeBase kb = KnowledgeBase.builder()
            .owner(userRepository.getReferenceById(user.userId()))
            .name(req.name())
            .description(req.description())
            .build();
        return toResponse(kbRepository.save(kb));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        var kbOpt = kbRepository.findById(id)
            .filter(kb -> kb.getOwner().getId().equals(user.userId()));
        if (kbOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        documentRepository.deleteAll(documentRepository.findByKbIdOrderByCreatedAtDesc(id));
        kbRepository.delete(kbOpt.get());
        return ResponseEntity.ok().build();
    }

    private KbResponse toResponse(KnowledgeBase kb) {
        return new KbResponse(kb.getId(), kb.getName(), kb.getDescription(),
            documentRepository.countByKbId(kb.getId()), kb.getCreatedAt().toString());
    }
}
