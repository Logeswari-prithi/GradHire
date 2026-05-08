package com.gradhire.controller;

import com.gradhire.dto.*;
import com.gradhire.entity.Role;
import com.gradhire.repository.ResumeHistoryRepository;
import com.gradhire.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ExcelService excelService;
    private final StudentService studentService;
    private final NotificationService notificationService;
    private final AnalyticsService analyticsService;
    private final PlacementDriveService placementDriveService;
    private final ResumeHistoryRepository resumeHistoryRepository;

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<Long>> createUser(@Valid @RequestBody CreateUserRequest req, Authentication auth) {
        var user = userService.createUser(req, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("User created", user.getId()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<java.util.List<com.gradhire.entity.User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", userService.getAllUsers()));
    }

    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<java.util.List<com.gradhire.entity.User>>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success("Users found", userService.searchUsers(q)));
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<ApiResponse<java.util.List<com.gradhire.entity.User>>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved",
                userService.getUsersByRole(Role.valueOf(role.toUpperCase()))));
    }

    @PatchMapping("/users/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleUser(@PathVariable Long id, Authentication auth) {
        userService.toggleUserStatus(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("User status updated", null));
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id, null); // uses default Admin@1234
        return ResponseEntity.ok(ApiResponse.success("Password reset. User must change on next login.", null));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Long>> updateUser(@PathVariable Long id,
                                                      @RequestBody java.util.Map<String, String> body,
                                                      Authentication auth) {
        var user = userService.updateUser(id,
                body.get("fullName"), body.get("email"),
                body.get("phone"), body.get("department"),
                auth.getName());
        return ResponseEntity.ok(ApiResponse.success("User updated", user.getId()));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<AnalyticsDTO>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success("Stats loaded", analyticsService.getAnalytics()));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<java.util.List<com.gradhire.entity.Notification>>> getNotifications() {
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved",
                notificationService.getAllNotifications()));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success("Unread count", notificationService.getUnreadCount()));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("All marked as read", null));
    }

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<java.util.List<StudentDTO>>> getAllStudents() {
        return ResponseEntity.ok(ApiResponse.success("Students retrieved", studentService.getAllStudents()));
    }

    @GetMapping("/error-reports")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getErrorReports() {
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
            map.put("subject", r.getSubject());
            map.put("severity", r.getSeverity());
            map.put("status", r.getStatus());
            map.put("timestamp", r.getCreatedAt());
            map.put("resolvedAt", r.getResolvedAt());
            map.put("resolvedBy", r.getResolvedBy());
            map.put("adminReply", r.getAdminReply());
            responses.add(map);
        }
        return ResponseEntity.ok(ApiResponse.success("Reports retrieved", responses));
    }

    @PatchMapping("/error-reports/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> markReportResolved(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> payload,
            Authentication auth) {
        String reply = payload != null && payload.containsKey("adminReply") ? payload.get("adminReply") : "Resolved";
        notificationService.markReportAsResolved(id, reply, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Report marked as resolved", null));
    }

    @GetMapping("/upload-history")
    public ResponseEntity<ApiResponse<java.util.List<com.gradhire.entity.UploadHistory>>> getUploadHistory() {
        return ResponseEntity.ok(ApiResponse.success("Upload history", excelService.getUploadHistory()));
    }

    @DeleteMapping("/upload-history/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUploadHistory(@PathVariable Long id) {
        excelService.deleteUploadHistory(id);
        return ResponseEntity.ok(ApiResponse.success("Upload history deleted", null));
    }

    @PostMapping("/upload-students")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadStudents(
            @RequestParam("file") MultipartFile file, 
            @RequestParam(value = "batchYear", required = false) Integer batchYear,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Excel processed", excelService.uploadStudentsFromExcel(file, batchYear, auth.getName())));
    }

    @PutMapping("/upload-records/{registerNumber}")
    public ResponseEntity<ApiResponse<String>> updateExcelRecord(
            @PathVariable String registerNumber,
            @RequestBody CreateUserRequest req,
            Authentication auth) {
        String result = excelService.updateExcelRecord(registerNumber, req, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Record " + result.toLowerCase(), result));
    }

    @DeleteMapping("/upload-records/{registerNumber}")
    public ResponseEntity<ApiResponse<Void>> deleteExcelRecord(@PathVariable String registerNumber) {
        excelService.deleteExcelRecord(registerNumber);
        return ResponseEntity.ok(ApiResponse.success("Record deleted", null));
    }

    @GetMapping("/placement-drives")
    public ResponseEntity<ApiResponse<java.util.List<com.gradhire.entity.PlacementDrive>>> getPlacementDrives() {
        return ResponseEntity.ok(ApiResponse.success("Drives retrieved", placementDriveService.getAllDrives()));
    }

    @GetMapping("/resume-history")
    public ResponseEntity<ApiResponse<java.util.List<com.gradhire.entity.ResumeHistory>>> getResumeHistory() {
        return ResponseEntity.ok(ApiResponse.success("Resume history", resumeHistoryRepository.findAllByOrderByCreatedAtDesc()));
    }
}
