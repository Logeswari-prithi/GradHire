package com.gradhire.dto;

import lombok.Data;
import java.util.List;

@Data
public class BulkResumeRequestDTO {
    private List<String> registerNumbers;
    private Long batchId;
}
