package com.gradhire.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    private String username;

    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Role is required")
    private String role;

    private String phone;
    private String department;
    private String registerNumber;
    private Integer batchYear;

    // Extra student fields from Excel
    private Double cgpa;
    private String gender;
    private String tenthPercent;
    private String twelfthPercent;
    private String ugPercentage;
    private String skills;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.gradhire.util.StringToListDeserializer.class)
    private java.util.List<String> toolsAndTechnologies;
    private String domain;
    private String address;
    private String placementStatus;
    private Integer currentBacklogs;
    private Integer historyOfBacklogs;
    private String careerGap;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.gradhire.util.StringToListDeserializer.class)
    private java.util.List<String> internships;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.gradhire.util.StringToListDeserializer.class)
    private java.util.List<String> projects;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.gradhire.util.StringToListDeserializer.class)
    private java.util.List<String> certifications;
    private String languages;
    private java.time.LocalDate dob;
    private String linkedinUrl;
    private String githubUrl;
    private String batch;
    private String status;
}
