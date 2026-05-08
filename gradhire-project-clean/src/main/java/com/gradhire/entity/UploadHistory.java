package com.gradhire.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "upload_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String uploadedBy;

    private Integer batchYear;

    @Builder.Default
    private int totalRecords = 0;

    @Builder.Default
    private int successCount = 0;

    @Builder.Default
    private int updateCount = 0;

    @Builder.Default
    private int failedCount = 0;

    @Column(length = 500)
    private String status;

    @Column(updatable = false)
    private LocalDateTime uploadDate;

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
    }
}
