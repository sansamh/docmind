package com.docmind.document;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.docmind.auth.AuthUser;
import com.docmind.config.AppProperties;
import com.docmind.document.dto.DocumentDtos.DocumentResponse;
import com.docmind.kb.KnowledgeBase;
import com.docmind.kb.KnowledgeBaseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/kb/{kbId}/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final KnowledgeBaseRepository kbRepository;
    private final KbDocumentRepository documentRepository;
    private final DocumentProcessingService processingService;
    private final AppProperties appProperties;

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> list(@AuthenticationPrincipal AuthUser user,
                                                       @PathVariable Long kbId) {
        Optional<KnowledgeBase> kb = ownedKb(user, kbId);
        if (kb.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<DocumentResponse> docs = documentRepository.findByKbIdOrderByCreatedAtDesc(kbId).stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(docs);
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> upload(@AuthenticationPrincipal AuthUser user,
                                                   @PathVariable Long kbId,
                                                   @RequestParam("file") MultipartFile file) {
        if (ownedKb(user, kbId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            KbDocument doc = KbDocument.builder()
                .kbId(kbId)
                .fileName(file.getOriginalFilename())
                .fileType(extensionOf(file.getOriginalFilename()))
                .sizeBytes(file.getSize())
                .status(KbDocument.Status.PROCESSING)
                .build();
            doc = documentRepository.save(doc);

            Path dest = Path.of(appProperties.getUploadDir(), doc.getId() + "_" + doc.getFileName());
            Files.createDirectories(dest.getParent());
            file.transferTo(dest.toAbsolutePath());

            processingService.process(doc.getId());
            return ResponseEntity.ok(toResponse(doc));
        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{docId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user,
                                       @PathVariable Long kbId, @PathVariable Long docId) {
        if (ownedKb(user, kbId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<KbDocument> doc = documentRepository.findById(docId)
            .filter(d -> d.getKbId().equals(kbId));
        if (doc.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        processingService.deleteVectors(doc.get());
        try {
            Files.deleteIfExists(Path.of(appProperties.getUploadDir(),
                doc.get().getId() + "_" + doc.get().getFileName()));
        } catch (Exception e) {
            log.warn("Could not delete uploaded file for doc {}", docId, e);
        }
        documentRepository.delete(doc.get());
        return ResponseEntity.ok().build();
    }

    private Optional<KnowledgeBase> ownedKb(AuthUser user, Long kbId) {
        return kbRepository.findById(kbId)
            .filter(kb -> kb.getOwner().getId().equals(user.userId()));
    }

    private DocumentResponse toResponse(KbDocument doc) {
        return new DocumentResponse(doc.getId(), doc.getFileName(), doc.getFileType(),
            doc.getSizeBytes(), doc.getStatus().name(), doc.getChunkCount(),
            doc.getErrorMessage(), doc.getCreatedAt().toString());
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "";
    }
}
