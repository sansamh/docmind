package com.docmind.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KbDocumentRepository extends JpaRepository<KbDocument, Long> {

    List<KbDocument> findByKbIdOrderByCreatedAtDesc(Long kbId);

    long countByKbId(Long kbId);
}
