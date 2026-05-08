package com.gradhire.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    private String generatedBy;

    private LocalDateTime expirationDate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expirationDate == null) {
            expirationDate = LocalDateTime.now().plusDays(7); // Keep history for 7 days
        }
    }
}
