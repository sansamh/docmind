package com.docmind.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByKbIdAndUserIdOrderByIdAsc(Long kbId, Long userId);
}
