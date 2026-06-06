package com.exam.utility.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String toEmail;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 50)
    private String templateName;

    @Column(nullable = false)
    private boolean sent;

    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(length = 50)
    private String referenceId;
}
