package com.gradhire.dto;

import lombok.Data;
import java.util.List;

@Data
public class ResumeRequestDTO {
    private String tenthPercent;
    private String twelfthPercent;
    private Double cgpa;
    private String skills;
    private String domain;
    private List<String> departments;
    private List<Integer> batchYears;
    private Integer currentBacklogs;
    private Integer historyOfBacklogs;
}
