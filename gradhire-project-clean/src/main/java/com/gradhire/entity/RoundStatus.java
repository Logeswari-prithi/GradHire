package com.gradhire.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "round_statuses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoundStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "placement_id", nullable = false)
    private Placement placement;

    @Column(nullable = false)
    private String roundName;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private java.time.LocalDate interviewDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlacementStatus status = PlacementStatus.PENDING;

    @Column(length = 1000)
    private String feedback;

    @Column(length = 1000)
    private String notes;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = PlacementStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
