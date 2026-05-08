package com.gradhire.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class AnalyticsDTO {
    private long totalStudents;
    private long totalPlacements;
    private long selectedCount;
    private long pendingCount;
    private double placementPercentage;
    private long totalCompaniesVisited;
    private Map<String, Long> batchWiseCount;
    private Map<String, Long> companyWiseCount;
    private Map<String, Long> statusWiseCount;
    private Map<String, Long> departmentWiseCount;
    private Map<String, Long> departmentTotalCount;
}
