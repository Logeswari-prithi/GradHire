package com.gradhire.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PlacementDTO {
    private Long id;
    private Long studentId;
    private String studentName;
    private String registerNumber;
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

    private String overallStatus;
    private String notes;
    private String remarks;
    private String department;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
