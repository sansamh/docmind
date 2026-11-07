package com.docmind.kb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class KbDtos {

    public record KbRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description) {
    }

    public record KbResponse(Long id, String name, String description, long documentCount, String createdAt) {
    }
}
