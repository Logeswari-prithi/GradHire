package com.gradhire.service;

import com.gradhire.dto.StudentDTO;
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
@org.springframework.transaction.annotation.Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public List<StudentDTO> getAllStudents() {
        return studentRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public StudentDTO getStudentById(Long id) {
        return toDTO(studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id)));
    }

    public StudentDTO getStudentByUserId(Long userId) {
        return toDTO(studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found")));
    }

    public StudentDTO getStudentByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return toDTO(studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user: " + username)));
    }

    @Transactional
    public StudentDTO updateStudent(Long id, StudentDTO dto, String modifiedBy) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));

        // String fields: update only if non-null and non-blank
        if (hasValue(dto.getDepartment())) student.setDepartment(dto.getDepartment());
        if (dto.getCgpa() != null) student.setCgpa(dto.getCgpa());
        if (hasValue(dto.getSkills())) student.setSkills(dto.getSkills());
        if (dto.getToolsAndTechnologies() != null) student.setToolsAndTechnologies(dto.getToolsAndTechnologies());
        if (hasValue(dto.getDomain())) student.setDomain(dto.getDomain());
        if (hasValue(dto.getTenthPercent())) student.setTenthPercent(dto.getTenthPercent());
        if (hasValue(dto.getTwelfthPercent())) student.setTwelfthPercent(dto.getTwelfthPercent());
        if (hasValue(dto.getUgPercentage())) student.setUgPercentage(dto.getUgPercentage());
        if (hasValue(dto.getAddress())) student.setAddress(dto.getAddress());
        if (hasValue(dto.getGender())) student.setGender(dto.getGender());
        if (dto.getDob() != null) student.setDob(dto.getDob());
        if (hasValue(dto.getLinkedinUrl())) student.setLinkedinUrl(dto.getLinkedinUrl());
        if (hasValue(dto.getGithubUrl())) student.setGithubUrl(dto.getGithubUrl());
        // PlacementStatus: allow setting to empty (clearing) or to a value
        if (dto.getPlacementStatus() != null) student.setPlacementStatus(dto.getPlacementStatus().isBlank() ? null : dto.getPlacementStatus());
        if (hasValue(dto.getBatch())) student.setBatchValue(dto.getBatch());
        if (hasValue(dto.getStatus())) student.setStatus(dto.getStatus());
        if (hasValue(dto.getProfilePhotoUrl())) student.setProfilePhotoUrl(dto.getProfilePhotoUrl());
        if (dto.getCurrentBacklogs() != null) student.setCurrentBacklogs(dto.getCurrentBacklogs());
        if (dto.getHistoryOfBacklogs() != null) student.setHistoryOfBacklogs(dto.getHistoryOfBacklogs());
        if (hasValue(dto.getCareerGap())) student.setCareerGap(dto.getCareerGap());

        // Resume fields
        if (hasValue(dto.getCareerObjective())) student.setCareerObjective(dto.getCareerObjective());
        if (dto.getProjects() != null) student.setProjects(dto.getProjects());
        if (dto.getCertifications() != null) student.setCertifications(dto.getCertifications());
        if (dto.getInternships() != null) student.setInternships(dto.getInternships());
        if (hasValue(dto.getAchievements())) student.setAchievements(dto.getAchievements());
        if (hasValue(dto.getLanguages())) student.setLanguages(dto.getLanguages());
        if (hasValue(dto.getHobbies())) student.setHobbies(dto.getHobbies());
        if (hasValue(dto.getDeclaration())) student.setDeclaration(dto.getDeclaration());

        student.setModifiedBy(modifiedBy);

        // Batch association: find or create batch
        try {
            if (dto.getBatchId() != null) {
                Batch batch = batchRepository.findById(dto.getBatchId())
                        .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));
                student.setBatch(batch);
            } else if (dto.getBatchYear() != null) {
                Batch batch = batchRepository.findByYear(dto.getBatchYear())
                        .orElseGet(() -> batchRepository.save(Batch.builder()
                                .year(dto.getBatchYear())
                                .description("Batch of " + dto.getBatchYear())
                                .build()));
                student.setBatch(batch);
            }
        } catch (Exception e) {
            // Log batch error but don't fail the entire update
            System.err.println("Batch update warning for student " + id + ": " + e.getMessage());
        }

        // Update related user fields
        User user = student.getUser();
        if (hasValue(dto.getPhone())) user.setPhone(dto.getPhone());
        if (hasValue(dto.getEmail())) user.setEmail(dto.getEmail());
        if (hasValue(dto.getFullName())) user.setFullName(dto.getFullName());
        userRepository.save(user);

        Student saved = studentRepository.save(student);
        notificationService.createNotificationByUsername(modifiedBy, "Student Updated", "Updated student profile: " + saved.getRegisterNumber(), "STUDENT_UPDATE");

        return toDTO(saved);
    }

    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    @Transactional
    public void deleteStudent(Long id, String deletedBy) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
        String regNo = student.getRegisterNumber();
        studentRepository.delete(student);
        notificationService.createNotificationByUsername(deletedBy, "Student Deleted", "Deleted student record: " + regNo, "STUDENT_DELETE");
    }

    public List<StudentDTO> searchStudents(String q) {
        return studentRepository.searchStudents(q).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<StudentDTO> getStudentsByBatch(Long batchId) {
        return studentRepository.findByBatchId(batchId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<StudentDTO> getStudentsByDepartment(String dept) {
        return studentRepository.findByDepartment(dept).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<StudentDTO> getStudentsByUpload(Long uploadHistoryId) {
        return studentRepository.findByUploadHistoryId(uploadHistoryId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<String> getDepartments() {
        return studentRepository.findDistinctDepartments();
    }

    public StudentDTO toDTO(Student s) {
        StudentDTO dto = new StudentDTO();
        dto.setId(s.getId());
        dto.setRegisterNumber(s.getRegisterNumber());
        dto.setFullName(s.getUser().getFullName());
        dto.setEmail(s.getUser().getEmail());
        dto.setPhone(s.getUser().getPhone());
        dto.setDepartment(s.getDepartment());
        dto.setCgpa(s.getCgpa());
        dto.setSkills(s.getSkills());
        dto.setToolsAndTechnologies(s.getToolsAndTechnologies());
        dto.setDomain(s.getDomain());
        dto.setTenthPercent(s.getTenthPercent());
        dto.setTwelfthPercent(s.getTwelfthPercent());
        dto.setUgPercentage(s.getUgPercentage());
        dto.setAddress(s.getAddress());
        dto.setGender(s.getGender());
        dto.setDob(s.getDob());
        dto.setLinkedinUrl(s.getLinkedinUrl());
        dto.setGithubUrl(s.getGithubUrl());
        dto.setCurrentBacklogs(s.getCurrentBacklogs());
        dto.setHistoryOfBacklogs(s.getHistoryOfBacklogs());
        dto.setCareerGap(s.getCareerGap());
        dto.setPlacementStatus(s.getPlacementStatus());
        dto.setBatch(s.getBatchValue());
        dto.setStatus(s.getStatus());
        dto.setUploadHistoryId(s.getUploadHistoryId());
        dto.setUserId(s.getUser().getId());
        if (s.getBatch() != null) {
            dto.setBatchYear(s.getBatch().getYear());
            dto.setBatchId(s.getBatch().getId());
        }
        // Resume fields
        dto.setCareerObjective(s.getCareerObjective());
        dto.setProjects(s.getProjects());
        dto.setCertifications(s.getCertifications());
        dto.setInternships(s.getInternships());
        dto.setAchievements(s.getAchievements());
        dto.setLanguages(s.getLanguages());
        dto.setHobbies(s.getHobbies());
        dto.setDeclaration(s.getDeclaration());
        dto.setProfilePhotoUrl(s.getProfilePhotoUrl());
        return dto;
    }

    public java.util.Map<String, Object> getDashboardStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        long totalStudents = studentRepository.count();
        long totalSelected = studentRepository.countByPlacementStatus("SELECTED");
        long totalPlacements = studentRepository.countByPlacementStatusNot("PENDING"); // Simplification

        stats.put("totalStudents", totalStudents);
        stats.put("totalSelected", totalSelected);
        stats.put("totalPlacements", totalPlacements);
        stats.put("placementPercentage", totalStudents > 0 ? (totalSelected * 100.0 / totalStudents) : 0);

        // Chart data
        stats.put("placementStatusDistribution", studentRepository.getPlacementStatusDistribution());
        stats.put("batchWiseDistribution", studentRepository.getBatchWiseDistribution());
        stats.put("departmentDistribution", studentRepository.getDepartmentDistribution());

        return stats;
    }
}
