package com.gradhire.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "students")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String registerNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    private String department;
    @Column(name = "cgpa", nullable = true)
    private Double cgpa;

    @Column(name = "batch", nullable = true)
    private String batchValue;

    @Column(name = "status", nullable = true)
    private String status;
    private String tenthPercent;
    private String twelfthPercent;
    private String ugPercentage;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String skills;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> toolsAndTechnologies = new java.util.ArrayList<>();

    private String domain;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String address;
    private String gender;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;
    private String phone;
    private String email;
    private String linkedinUrl;
    private String githubUrl;
    @Builder.Default
    private Integer currentBacklogs = 0;
    @Builder.Default
    @Column(name = "active_backlog")
    private Integer activeBacklog = 0;
    @Builder.Default
    private Integer historyOfBacklogs = 0;
    private String careerGap;
    private String placementStatus;
    private String profilePhotoUrl;

    // Resume-specific fields
    @Lob
    @Column(columnDefinition = "TEXT")
    private String careerObjective;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> projects = new java.util.ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> certifications = new java.util.ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private List<String> internships = new java.util.ArrayList<>();

    @Lob
    @Column(columnDefinition = "TEXT")
    private String achievements;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String languages;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String hobbies;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String declaration;

    private Long uploadHistoryId;

    private String modifiedBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
