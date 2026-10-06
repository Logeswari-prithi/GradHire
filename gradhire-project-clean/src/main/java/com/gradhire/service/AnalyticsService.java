package com.gradhire.service;

import com.gradhire.dto.AnalyticsDTO;
import com.gradhire.entity.PlacementStatus;
import com.gradhire.entity.Student;
import com.gradhire.repository.BatchRepository;
import com.gradhire.repository.PlacementDriveRepository;
import com.gradhire.repository.PlacementRepository;
import com.gradhire.repository.StudentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final StudentRepository studentRepository;
    private final PlacementRepository placementRepository;
    private final BatchRepository batchRepository;
    private final PlacementDriveRepository placementDriveRepository;

    public AnalyticsDTO getAnalytics() {

        // Total counts
        long totalStudents = studentRepository.count();
        long totalPlacements = placementRepository.count();

        // Total companies visited (unique drives)
        long totalCompaniesVisited = placementDriveRepository.count();

        // Fetch all data
        List<Student> allStudents = studentRepository.findAll();
        List<com.gradhire.entity.Placement> allPlacements = placementRepository.findAll();
        Map<Long, List<com.gradhire.entity.Placement>> placementsByStudent = allPlacements.stream()
                .filter(p -> p.getStudent() != null)
                .collect(Collectors.groupingBy(p -> p.getStudent().getId()));

        java.util.function.Function<Student, String> getEffectiveStatus = (s) -> {
            List<com.gradhire.entity.Placement> pList = placementsByStudent.getOrDefault(s.getId(), Collections.emptyList());
            if (pList.isEmpty()) return "Unselected";
            if (pList.stream().anyMatch(p -> p.getOverallStatus() == PlacementStatus.SELECTED)) return "Selected";
            
            com.gradhire.entity.Placement latest = pList.stream()
                .max(Comparator.comparing(p -> p.getUpdatedAt() != null ? p.getUpdatedAt() : java.time.LocalDateTime.MIN))
                .orElse(null);
                
            if (latest == null || latest.getOverallStatus() == null) return "Unselected";
            
            switch (latest.getOverallStatus()) {
                case SELECTED: return "Selected";
                case REJECTED: return "Rejected";
                case APPLIED: return "Applied";
                case PENDING: return "Pending";
                case WAITING: return "Waiting";
                default: return "Unselected";
            }
        };

        long selectedCount = allStudents.stream()
                .filter(s -> "Selected".equals(getEffectiveStatus.apply(s)))
                .count();

        long pendingCount = allStudents.stream()
                .filter(s -> "Pending".equals(getEffectiveStatus.apply(s)) || "Waiting".equals(getEffectiveStatus.apply(s)))
                .count();

        long rejectedCount = allStudents.stream()
                .filter(s -> "Rejected".equals(getEffectiveStatus.apply(s)))
                .count();

        long eligibleBase = allStudents.size() - rejectedCount;

        // Placement percentage
        double placementPercent = eligibleBase > 0
                ? Math.round((double) selectedCount / eligibleBase * 10000.0) / 100.0
                : 0.0;

        // Batch-wise selected count AND total count AND detailed counts
        Map<String, Long> batchWiseSelected = new LinkedHashMap<>();
        Map<String, Long> batchWiseTotal = new LinkedHashMap<>();
        Map<String, Map<String, Long>> batchWiseDetailedCount = new LinkedHashMap<>();

        batchRepository.findAll().stream()
                .sorted(Comparator.comparing(b -> b.getYear()))
                .forEach(b -> {
                    List<Student> students = studentRepository.findByBatchId(b.getId());
                    String batchStr = String.valueOf(b.getYear());
                    batchWiseTotal.put(batchStr, (long) students.size());
                    batchWiseSelected.put(batchStr, students.stream()
                            .filter(s -> "Selected".equals(getEffectiveStatus.apply(s)))
                            .count());

                    Map<String, Long> details = new LinkedHashMap<>();
                    details.put("Selected", students.stream().filter(s -> "Selected".equals(getEffectiveStatus.apply(s))).count());
                    details.put("Rejected", students.stream().filter(s -> "Rejected".equals(getEffectiveStatus.apply(s))).count());
                    details.put("Applied", students.stream().filter(s -> "Applied".equals(getEffectiveStatus.apply(s))).count());
                    details.put("Waiting", students.stream().filter(s -> "Waiting".equals(getEffectiveStatus.apply(s))).count());
                    details.put("Pending", students.stream().filter(s -> "Pending".equals(getEffectiveStatus.apply(s))).count());
                    details.put("Unselected", students.stream().filter(s -> "Unselected".equals(getEffectiveStatus.apply(s))).count());
                    
                    batchWiseDetailedCount.put(batchStr, details);
                });

        // Company-wise placement count
        Map<String, Long> companyWise = new LinkedHashMap<>();
        placementRepository.getPlacementsByCompany()
                .forEach(row ->
                        companyWise.put(
                                (String) row[0],
                                (Long) row[1]
                        )
                );

        // Status-wise count (from placements table)
        Map<String, Long> statusWise = new LinkedHashMap<>();
        for (PlacementStatus s : PlacementStatus.values()) {
            statusWise.put(
                    s.name(),
                    (long) placementRepository.findByOverallStatus(s).size()
            );
        }

        // Student-level placement status distribution
        Map<String, Long> studentPlacementStatus = new LinkedHashMap<>();
        Map<String, Long> rawStudentStatus = allStudents.stream()
                .collect(Collectors.groupingBy(
                        getEffectiveStatus,
                        Collectors.counting()
                ));

        // Order: Selected, Rejected, Applied, Waiting, Pending, Unselected
        studentPlacementStatus.put("Selected", rawStudentStatus.getOrDefault("Selected", 0L));
        studentPlacementStatus.put("Rejected", rawStudentStatus.getOrDefault("Rejected", 0L));
        studentPlacementStatus.put("Applied", rawStudentStatus.getOrDefault("Applied", 0L));
        studentPlacementStatus.put("Waiting", rawStudentStatus.getOrDefault("Waiting", 0L));
        studentPlacementStatus.put("Pending", rawStudentStatus.getOrDefault("Pending", 0L));
        studentPlacementStatus.put("Unselected", rawStudentStatus.getOrDefault("Unselected", 0L));

        // Department-wise PLACED count (for color coding)
        Map<String, Long> deptWise = allStudents.stream()
                .filter(s ->
                        s.getDepartment() != null &&
                                !s.getDepartment().isBlank() &&
                                "Selected".equals(getEffectiveStatus.apply(s))
                )
                .collect(Collectors.groupingBy(
                        Student::getDepartment,
                        Collectors.counting()
                ));

        // Department-wise TOTAL count (for distribution)
        Map<String, Long> deptTotal = allStudents.stream()
                .filter(s -> s.getDepartment() != null && !s.getDepartment().isBlank())
                .collect(Collectors.groupingBy(
                        Student::getDepartment,
                        Collectors.counting()
                ));

        // Year-wise trends: avg CGPA, selection count, total students, selection rate per batch
        List<Map<String, Object>> yearWiseTrends = new ArrayList<>();
        batchRepository.findAll().stream()
                .sorted(Comparator.comparing(b -> b.getYear()))
                .forEach(b -> {
                    List<Student> bStudents = studentRepository.findByBatchId(b.getId());
                    if (bStudents.isEmpty()) return;

                    long total = bStudents.size();
                    long selected = bStudents.stream()
                            .filter(s -> "Selected".equals(getEffectiveStatus.apply(s)))
                            .count();
                    double avgCgpa = bStudents.stream()
                            .filter(s -> s.getCgpa() != null && s.getCgpa() > 0)
                            .mapToDouble(Student::getCgpa)
                            .average()
                            .orElse(0.0);
                    avgCgpa = Math.round(avgCgpa * 100.0) / 100.0;
                    double selectionRate = total > 0
                            ? Math.round((double) selected / total * 10000.0) / 100.0
                            : 0.0;

                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("year", b.getYear());
                    entry.put("totalStudents", total);
                    entry.put("selectedCount", selected);
                    entry.put("avgCgpa", avgCgpa);
                    entry.put("selectionRate", selectionRate);
                    yearWiseTrends.add(entry);
                });

        // Build DTO
        return AnalyticsDTO.builder()
                .totalStudents(totalStudents)
                .totalPlacements(totalPlacements)
                .selectedCount(selectedCount)
                .pendingCount(pendingCount)
                .placementPercentage(placementPercent)
                .totalCompaniesVisited(totalCompaniesVisited)
                .batchWiseCount(batchWiseSelected)
                .batchWiseTotalCount(batchWiseTotal)
                .companyWiseCount(companyWise)
                .statusWiseCount(statusWise)
                .studentPlacementStatusCount(studentPlacementStatus)
                .departmentWiseCount(deptWise)
                .departmentTotalCount(deptTotal)
                .yearWiseTrends(yearWiseTrends)
                .batchWiseDetailedCount(batchWiseDetailedCount)
                .build();
    }
}