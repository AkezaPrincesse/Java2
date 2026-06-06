package com.exam.utility.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_codes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private LocalDateTime expiryAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Builder.Default
    private int attemptCount = 0;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryAt);
    }
}
