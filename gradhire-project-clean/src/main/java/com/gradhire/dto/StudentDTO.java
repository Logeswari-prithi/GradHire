package com.gradhire.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class StudentDTO {
    private Long id;
    private String registerNumber;
    private String fullName;
    private String email;
    private String phone;
    private String department;
    private Double cgpa;
    private String skills;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.gradhire.util.StringToListDeserializer.class)
    private java.util.List<String> toolsAndTechnologies;
    private String domain;
    private String tenthPercent;
    private String twelfthPercent;
    private String ugPercentage;
    private String address;
    private String gender;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;
    private String linkedinUrl;
    private String githubUrl;
    private Integer currentBacklogs;
    private Integer historyOfBacklogs;
    private String careerGap;
    private Integer batchYear;
    private Long batchId;
    private String placementStatus;
    private String batch;
    private String status;
    private String profilePhotoUrl;

    // Resume fields
    private String careerObjective;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.gradhire.util.StringToListDeserializer.class)
    private java.util.List<String> projects;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.gradhire.util.StringToListDeserializer.class)
    private java.util.List<String> certifications;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.gradhire.util.StringToListDeserializer.class)
    private java.util.List<String> internships;
    private String achievements;
    private String languages;
    private String hobbies;
    private String declaration;

    private Long uploadHistoryId;
    private Long userId;
}
