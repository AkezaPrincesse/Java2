package com.exam.utility.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemSetting extends BaseEntity {

    @Column(unique = true, nullable = false, length = 100)
    private String settingKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(length = 255)
    private String description;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    @Builder.Default
    private boolean editable = true;
}
