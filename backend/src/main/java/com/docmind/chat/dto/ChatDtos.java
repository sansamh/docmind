package com.docmind.chat.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatDtos {

    public record ChatRequest(
        @NotBlank @Size(max = 4000) String question) {
    }

    public record SourceChunk(int index, String docName, String snippet, Double score) {
    }

    public record MessageResponse(Long id, String role, String content, List<SourceChunk> sources, String createdAt) {
    }
}
