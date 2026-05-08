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
        long selectedCount = studentRepository.countByPlacementStatus("SELECTED");
        long pendingCount =
                placementRepository.findByOverallStatus(PlacementStatus.PENDING).size();

        // Total companies visited (unique drives)
        long totalCompaniesVisited = placementDriveRepository.count();

        // Placement percentage
        double placementPercent = totalStudents > 0
                ? Math.round((double) selectedCount / totalStudents * 10000.0) / 100.0
                : 0.0;

        // Batch-wise placed count
        Map<String, Long> batchWise = new LinkedHashMap<>();
        batchRepository.findAll().stream()
                .sorted(Comparator.comparing(b -> b.getYear()))
                .forEach(b ->
                        batchWise.put(
                                String.valueOf(b.getYear()),
                                studentRepository.findByBatchId(b.getId()).stream()
                                        .filter(s -> "SELECTED".equalsIgnoreCase(s.getPlacementStatus()))
                                        .count()
                        )
                );

        // Company-wise placement count
        Map<String, Long> companyWise = new LinkedHashMap<>();
        placementRepository.getPlacementsByCompany()
                .forEach(row ->
                        companyWise.put(
                                (String) row[0],
                                (Long) row[1]
                        )
                );

        // Status-wise count
        Map<String, Long> statusWise = new LinkedHashMap<>();
        for (PlacementStatus s : PlacementStatus.values()) {
            statusWise.put(
                    s.name(),
                    (long) placementRepository.findByOverallStatus(s).size()
            );
        }

        // Department-wise PLACED count (for color coding)
        Map<String, Long> deptWise = studentRepository.findAll().stream()
                .filter(s ->
                        s.getDepartment() != null &&
                                !s.getDepartment().isBlank() &&
                                "SELECTED".equalsIgnoreCase(s.getPlacementStatus())
                )
                .collect(Collectors.groupingBy(
                        Student::getDepartment,
                        Collectors.counting()
                ));

        // Department-wise TOTAL count (for distribution)
        Map<String, Long> deptTotal = studentRepository.findAll().stream()
                .filter(s -> s.getDepartment() != null && !s.getDepartment().isBlank())
                .collect(Collectors.groupingBy(
                        Student::getDepartment,
                        Collectors.counting()
                ));

        // Build DTO
        return AnalyticsDTO.builder()
                .totalStudents(totalStudents)
                .totalPlacements(totalPlacements)
                .selectedCount(selectedCount)
                .pendingCount(pendingCount)
                .placementPercentage(placementPercent)
                .totalCompaniesVisited(totalCompaniesVisited)
                .batchWiseCount(batchWise)
                .companyWiseCount(companyWise)
                .statusWiseCount(statusWise)
                .departmentWiseCount(deptWise)
                .departmentTotalCount(deptTotal)
                .build();
    }
}