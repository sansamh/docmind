package com.docmind.kb;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    List<KnowledgeBase> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
