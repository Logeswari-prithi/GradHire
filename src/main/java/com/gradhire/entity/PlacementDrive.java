package com.gradhire.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "placement_drives")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementDrive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    private String jobRole;
    private Double packageOffered;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate driveDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate applicationDeadline;

    // Upcoming Companies fields
    @Column(length = 1000)
    private String skills;

    private Double minCgpa;

    private Integer batch;

    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    @Transient
    private String status;
    
    @Transient
    private Long studentsAppliedCount;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @PostLoad
    protected void calculateStatus() {
        if (applicationDeadline != null && LocalDate.now().isAfter(applicationDeadline)) {
            this.status = "EXPIRED";
        } else {
            this.status = "ONGOING";
        }
    }
}
