package com.gradhire.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "placements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Placement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drive_id")
    private PlacementDrive drive;

    @Column(nullable = false)
    private String companyName;

    private String jobRole;
    private String jobLocation;
    private Double packageOffered;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate applicationDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate interviewDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate offerDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlacementStatus overallStatus = PlacementStatus.PENDING;

    @Column(length = 1000)
    private String notes;

    @Column(length = 1000)
    private String remarks;

    private String department;

    private String modifiedBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (overallStatus == null) overallStatus = PlacementStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
