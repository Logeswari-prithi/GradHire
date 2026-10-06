package com.gradhire.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
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
    private Map<String, Long> batchWiseCount;        // batch -> selected count (kept for backward compat)
    private Map<String, Long> batchWiseTotalCount;   // batch -> total student count
    private Map<String, Long> companyWiseCount;
    private Map<String, Long> statusWiseCount;
    private Map<String, Long> studentPlacementStatusCount; // student-level: SELECTED, NOT_ATTENDED, PENDING, Not Placed
    private Map<String, Long> departmentWiseCount;
    private Map<String, Long> departmentTotalCount;
    private List<Map<String, Object>> yearWiseTrends; // [{year, avgCgpa, selectedCount, totalStudents, selectionRate}]
    private Map<String, Map<String, Long>> batchWiseDetailedCount; // batch -> {Rejected, Applied, Waiting, Unselected}
}
