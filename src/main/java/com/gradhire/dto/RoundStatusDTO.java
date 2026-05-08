package com.gradhire.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RoundStatusDTO {
    private Long id;
    private Long placementId;
    private String roundName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate interviewDate;

    private String status;
    private String feedback;
    private String notes;
}
