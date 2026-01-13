package com.taskqueue.www.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class GeneratedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long taskId;

    private String filename;

    private String documentType; // invoice, receipt, report

    private String storageType; // LOCAL, S3, DATABASE

    @Column(length = 1000)
    private String storagePath;

    private String s3Bucket;

    private String s3Key;

    private Long fileSizeBytes;

    private String mimeType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime expiresAt;

    private Boolean isDeleted = false;

    @Column(length = 2000)
    private String metadata;

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}