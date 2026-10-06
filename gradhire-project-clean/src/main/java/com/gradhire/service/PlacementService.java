package com.gradhire.service;

import com.gradhire.dto.PlacementDTO;
import com.gradhire.entity.*;
import com.gradhire.exception.ResourceNotFoundException;
import com.gradhire.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlacementService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PlacementService.class);

    private final PlacementRepository placementRepository;
    private final StudentRepository studentRepository;
    private final RoundStatusRepository roundStatusRepository;
    private final PlacementActionLogRepository actionLogRepository;
    private final NotificationService notificationService;

    private void logAction(Long placementId, Long studentId, String actionType, String entityType, String details, String performedBy) {
        actionLogRepository.save(PlacementActionLog.builder()
                .placementId(placementId)
                .studentId(studentId)
                .actionType(actionType)
                .entityType(entityType)
                .details(details)
                .performedBy(performedBy)
                .build());
        notificationService.createNotificationByUsername(performedBy, "Placement Update", details, "PLACEMENT");
    }

    @Transactional
    public PlacementDTO createPlacement(PlacementDTO dto, String createdBy) {
        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + dto.getStudentId()));

        PlacementStatus status = PlacementStatus.PENDING;
        if (dto.getOverallStatus() != null && !dto.getOverallStatus().isBlank()) {
            try { status = PlacementStatus.valueOf(dto.getOverallStatus()); } catch (Exception ignored) {}
        }

        Placement placement = Placement.builder()
                .student(student)
                .companyName(dto.getCompanyName())
                .jobRole(dto.getJobRole())
                .jobLocation(dto.getJobLocation())
                .packageOffered(dto.getPackageOffered())
                .applicationDate(dto.getApplicationDate())
                .interviewDate(dto.getInterviewDate())
                .offerDate(dto.getOfferDate())
                .overallStatus(status)
                .notes(dto.getNotes())
                .remarks(dto.getRemarks())
                .department(student.getDepartment())
                .modifiedBy(createdBy)
                .build();

        Placement saved = placementRepository.save(placement);
        logAction(saved.getId(), student.getId(), "CREATE", "PLACEMENT", "Placement created for " + dto.getCompanyName(), createdBy);
        syncStudentPlacementStatus(student);
        return toDTO(saved);
    }

    @Transactional
    public PlacementDTO updatePlacement(Long id, PlacementDTO dto, String modifiedBy) {
        Placement p = placementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement not found: " + id));

        // Only update fields that are actually provided (non-null)
        if (dto.getCompanyName() != null && !dto.getCompanyName().isBlank()) p.setCompanyName(dto.getCompanyName());
        if (dto.getJobRole() != null) p.setJobRole(dto.getJobRole());
        if (dto.getJobLocation() != null) p.setJobLocation(dto.getJobLocation());
        if (dto.getPackageOffered() != null) p.setPackageOffered(dto.getPackageOffered());
        if (dto.getApplicationDate() != null) p.setApplicationDate(dto.getApplicationDate());
        if (dto.getInterviewDate() != null) p.setInterviewDate(dto.getInterviewDate());
        if (dto.getOfferDate() != null) p.setOfferDate(dto.getOfferDate());
        if (dto.getOverallStatus() != null && !dto.getOverallStatus().isBlank()) {
            try { p.setOverallStatus(PlacementStatus.valueOf(dto.getOverallStatus())); } catch (Exception ignored) {}
        }
        if (dto.getNotes() != null) p.setNotes(dto.getNotes());
        if (dto.getRemarks() != null) p.setRemarks(dto.getRemarks());
        p.setModifiedBy(modifiedBy);
        Placement saved = placementRepository.save(p);
        logAction(p.getId(), p.getStudent().getId(), "UPDATE", "PLACEMENT", "Placement updated", modifiedBy);
        syncStudentPlacementStatus(p.getStudent());
        return toDTO(saved);
    }

    @Transactional
    public void deletePlacement(Long id, String deletedBy) {
        Placement p = placementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Placement not found: " + id));
        // Delete associated rounds first
        roundStatusRepository.findByPlacementId(id).forEach(roundStatusRepository::delete);
        Long studentId = p.getStudent().getId();
        Student student = p.getStudent();
        String company = p.getCompanyName();
        placementRepository.delete(p);
        logAction(id, studentId, "DELETE", "PLACEMENT", "Placement deleted for " + company, deletedBy);
        syncStudentPlacementStatus(student);
    }

    @Transactional
    public void createRound(Long placementId, com.gradhire.dto.RoundStatusDTO dto, String createdBy) {
        Placement p = placementRepository.findById(placementId)
                .orElseThrow(() -> new ResourceNotFoundException("Placement not found"));

        PlacementStatus roundStatus = PlacementStatus.PENDING;
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            try { roundStatus = PlacementStatus.valueOf(dto.getStatus()); } catch (Exception ignored) {}
        }

        RoundStatus round = RoundStatus.builder()
                .placement(p)
                .roundName(dto.getRoundName())
                .interviewDate(dto.getInterviewDate())
                .status(roundStatus)
                .feedback(dto.getFeedback())
                .notes(dto.getNotes())
                .build();
        roundStatusRepository.save(round);
        logAction(placementId, p.getStudent().getId(), "CREATE", "ROUND", "Round created: " + dto.getRoundName(), createdBy);
    }

    @Transactional
    public void updateRound(Long roundId, com.gradhire.dto.RoundStatusDTO dto, String updatedBy) {
        RoundStatus round = roundStatusRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found: " + roundId));
        if (dto.getRoundName() != null) round.setRoundName(dto.getRoundName());
        if (dto.getInterviewDate() != null) round.setInterviewDate(dto.getInterviewDate());
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            try { round.setStatus(PlacementStatus.valueOf(dto.getStatus())); } catch (Exception ignored) {}
        }
        if (dto.getFeedback() != null) round.setFeedback(dto.getFeedback());
        if (dto.getNotes() != null) round.setNotes(dto.getNotes());
        roundStatusRepository.save(round);
        logAction(round.getPlacement().getId(), round.getPlacement().getStudent().getId(), "UPDATE", "ROUND", "Round updated: " + round.getRoundName(), updatedBy);
    }

    @Transactional
    public void deleteRound(Long roundId, String deletedBy) {
        RoundStatus round = roundStatusRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Round not found: " + roundId));
        Long pId = round.getPlacement().getId();
        Long sId = round.getPlacement().getStudent().getId();
        String rName = round.getRoundName();
        roundStatusRepository.delete(round);
        logAction(pId, sId, "DELETE", "ROUND", "Round deleted: " + rName, deletedBy);
    }

    public java.util.Map<String, Object> getPlacementHistory(Long studentId) {
        List<PlacementDTO> placements = getPlacementsByStudent(studentId);
        List<PlacementActionLog> logs = actionLogRepository.findByStudentIdOrderByTimestampDesc(studentId);
        return java.util.Map.of("placements", placements, "logs", logs);
    }

    public List<PlacementDTO> getPlacementsByStudent(Long studentId) {
        return placementRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<PlacementDTO> getAllPlacements() {
        return placementRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<PlacementDTO> searchPlacements(String q) {
        return placementRepository.searchPlacements(q).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<RoundStatus> getRoundsForPlacement(Long placementId) {
        return roundStatusRepository.findByPlacementId(placementId);
    }

    private PlacementDTO toDTO(Placement p) {
        PlacementDTO dto = new PlacementDTO();
        dto.setId(p.getId());
        dto.setStudentId(p.getStudent().getId());
        dto.setStudentName(p.getStudent().getUser() != null ? p.getStudent().getUser().getFullName() : "Unknown");
        dto.setRegisterNumber(p.getStudent().getRegisterNumber());
        dto.setCompanyName(p.getCompanyName());
        dto.setJobRole(p.getJobRole());
        dto.setJobLocation(p.getJobLocation());
        dto.setPackageOffered(p.getPackageOffered());
        dto.setApplicationDate(p.getApplicationDate());
        dto.setInterviewDate(p.getInterviewDate());
        dto.setOfferDate(p.getOfferDate());
        dto.setOverallStatus(p.getOverallStatus() != null ? p.getOverallStatus().name() : "PENDING");
        dto.setNotes(p.getNotes());
        dto.setRemarks(p.getRemarks());
        dto.setDepartment(p.getStudent().getDepartment());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }

    private void syncStudentPlacementStatus(Student student) {
        List<Placement> placements = placementRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        if (placements.isEmpty()) {
            student.setPlacementStatus("Unselected");
        } else {
            boolean isSelected = placements.stream().anyMatch(p -> p.getOverallStatus() == PlacementStatus.SELECTED);
            if (isSelected) {
                student.setPlacementStatus("SELECTED");
            } else {
                student.setPlacementStatus(placements.get(0).getOverallStatus().name());
            }
        }
        studentRepository.save(student);
    }
}
