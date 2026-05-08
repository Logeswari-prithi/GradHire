package com.gradhire.controller;

import com.gradhire.dto.ApiResponse;
import com.gradhire.dto.ResumeRequestDTO;
import com.gradhire.entity.ResumeHistory;
import com.gradhire.repository.ResumeHistoryRepository;
import com.gradhire.service.ResumeService;
import lombok.RequiredArgsConstructor;


import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReportController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReportController.class);
    private final ResumeService resumeService;
    private final ResumeHistoryRepository resumeHistoryRepository;
    private final com.gradhire.repository.StudentRepository studentRepository;
    private final com.gradhire.repository.UserRepository userRepository;
    private final com.gradhire.service.NotificationService notificationService;
    private final com.gradhire.service.StudentService studentService;

    @GetMapping("/student/{studentId}/pdf")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long studentId, Authentication auth) {
        log.debug("Generating PDF for student: {}", studentId);
        if (auth == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        
        com.gradhire.entity.User authUser = userRepository.findByUsername(auth.getName())
                .orElse(null);
                
        if (authUser == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        
        com.gradhire.dto.StudentDTO s;
        try {
            s = studentService.getStudentById(studentId);
        } catch (Exception e) {
            log.error("Student not found: {}", studentId);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).build();
        }
        
        if (authUser.getRole() == com.gradhire.entity.Role.STUDENT && !authUser.getId().equals(s.getUserId())) {
            log.warn("Unauthorized access attempt. Student {} tried to download resume of {}", authUser.getId(), s.getUserId());
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }
        
        byte[] pdf;
        try {
            pdf = resumeService.generateResumePdf(studentId);
            if (authUser.getRole() == com.gradhire.entity.Role.STUDENT) {
                notificationService.createNotificationTargeted("System", authUser.getUsername(), "Resume Generated", "Your resume PDF has been successfully generated and downloaded.", "INFO");
            }
            notificationService.createNotificationByUsername(auth.getName(), "Resume Generated", "Generated single resume for " + s.getRegisterNumber(), "RESUME_GENERATE");
        } catch (Exception e) {
            log.error("Failed to generate PDF for student {}: {}", studentId, e.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        
        String regNo = s.getRegisterNumber() != null ? s.getRegisterNumber() : "UNKNOWN";
        String stuName = s.getFullName() != null ? s.getFullName().replaceAll("\\s+", "_") : "Student";
        String filename = regNo + "_" + stuName + "_Resume.pdf";
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/preview-bulk")
    public ResponseEntity<ApiResponse<Integer>> previewBulk(@RequestBody ResumeRequestDTO request) {
        try {
            java.util.List<com.gradhire.dto.StudentDTO> matched = resumeService.filterStudents(request);
            return ResponseEntity.ok(ApiResponse.success("Count retrieved", matched.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/bulk-resumes")
    public ResponseEntity<ApiResponse<Long>> generateBulk(@RequestBody ResumeRequestDTO request, Authentication auth) {
        try {
            ResumeHistory history = resumeService.generateBulkResumes(request, auth.getName());
            if (history == null) {
                return ResponseEntity.ok(ApiResponse.success("No students found matching the given criteria. Try broadening your filters.", null));
            }
            return ResponseEntity.ok(ApiResponse.success("Resumes generated successfully", history.getId()));
        } catch (Exception e) {
            log.error("Bulk resume generation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/download-bulk/{historyId}")
    public ResponseEntity<byte[]> downloadBulk(@PathVariable Long historyId) throws java.io.IOException {
        ResumeHistory history = resumeHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("History not found"));
        
        java.io.File file = new java.io.File(history.getFilePath());
        if (!file.exists()) {
             throw new RuntimeException("File not found on server");
        }
        byte[] content = java.nio.file.Files.readAllBytes(file.toPath());
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + history.getFileName() + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(content);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> submitReport(@RequestBody java.util.Map<String, String> payload, Authentication auth) {
        try {
            com.gradhire.entity.User authUser = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            String message = payload.getOrDefault("message", payload.get("description"));
            String subject = payload.getOrDefault("subject", "User Report");
            
            com.gradhire.entity.ErrorReport report = notificationService.submitErrorReport(
                authUser.getId(), subject, message, payload.getOrDefault("severity", "MEDIUM")
            );
            return ResponseEntity.ok(ApiResponse.success("Report submitted successfully", report.getId()));
        } catch (Exception e) {
            log.error("Report submission failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getAllReports() {
        java.util.List<java.util.Map<String, Object>> responses = new java.util.ArrayList<>();
        for (com.gradhire.entity.ErrorReport r : notificationService.getAllErrorReports()) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", r.getId());
            if (r.getUser() != null) {
                map.put("user", r.getUser().getFullName() != null ? r.getUser().getFullName() : r.getUser().getUsername());
                map.put("role", r.getUser().getRole().name());
            } else {
                map.put("user", "Unknown");
                map.put("role", "UNKNOWN");
            }
            map.put("message", r.getMessage() != null ? r.getMessage() : r.getSubject());
            map.put("status", r.getStatus());
            map.put("timestamp", r.getCreatedAt());
            map.put("subject", r.getSubject());
            map.put("severity", r.getSeverity());
            map.put("resolvedAt", r.getResolvedAt());
            map.put("resolvedBy", r.getResolvedBy());
            map.put("adminReply", r.getAdminReply());
            responses.add(map);
        }
        return ResponseEntity.ok(ApiResponse.success("Reports retrieved", responses));
    }

    @PatchMapping("/{id}/resolve")
    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resolveReport(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> payload, Authentication auth) {
        String reply = payload != null && payload.containsKey("adminReply") ? payload.get("adminReply") : "Resolved";
        notificationService.markReportAsResolved(id, reply, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Report marked as resolved", null));
    }
}
