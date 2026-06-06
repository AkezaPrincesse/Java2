package com.exam.utility.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "file_uploads")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileUpload extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 255)
    private String originalName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(length = 50)
    private String entityType;

    @Column(length = 50)
    private String entityId;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
