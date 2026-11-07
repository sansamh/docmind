package com.docmind.document;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbDocument {

    public enum Status { PROCESSING, READY, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long kbId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private int chunkCount;

    @Lob
    @Column(length = 100_000)
    private String chunkIdsJson;

    @Column(length = 1000)
    private String errorMessage;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
