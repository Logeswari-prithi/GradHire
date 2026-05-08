package com.gradhire.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementDriveDTO {
    private Long id;
    private String companyName;
    private String jobRole;
    private Double packageOffered;
    private LocalDate driveDate;
    private LocalDate applicationDeadline;
    private String skills;
    private Double minCgpa;
    private Integer batch;
    private String status;
    private Long studentsAppliedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
