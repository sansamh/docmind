package com.docmind.document.dto;

public class DocumentDtos {

    public record DocumentResponse(
        Long id, String fileName, String fileType, long sizeBytes,
        String status, int chunkCount, String errorMessage, String createdAt) {
    }
}
