package com.gradhire.controller;

import com.gradhire.dto.ApiResponse;
import com.gradhire.service.ExcelService;
import com.gradhire.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StaffController {

    private final ExcelService excelService;
    private final NotificationService notificationService;

    @PostMapping("/upload-students")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadStudents(
            @RequestParam("file") MultipartFile file, 
            @RequestParam(value = "batchYear", required = false) Integer batchYear,
            Authentication auth) {
        Map<String, Object> results = excelService.uploadStudentsFromExcel(file, batchYear, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Upload completed", results));
    }

    @PostMapping("/error-report")
    public ResponseEntity<ApiResponse<Void>> reportError(
            @RequestParam Long userId,
            @RequestParam String subject,
            @RequestParam String description,
            @RequestParam(defaultValue = "LOW") String severity) {
        notificationService.submitErrorReport(userId, subject, description, severity);
        return ResponseEntity.ok(ApiResponse.success("Error report submitted", null));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<java.util.List<com.gradhire.entity.Notification>>> getNotifications(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Notifications", notificationService.getGlobalAndUserNotifications(auth.getName())));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Count", notificationService.getUnreadCount(auth.getName())));
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication auth) {
        notificationService.markAllAsRead(auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id, Authentication auth) {
        notificationService.markAsRead(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
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

    @PutMapping("/upload-records/{registerNumber}")
    public ResponseEntity<ApiResponse<String>> updateExcelRecord(
            @PathVariable String registerNumber,
            @RequestBody com.gradhire.dto.CreateUserRequest req,
            Authentication auth) {
        String result = excelService.updateExcelRecord(registerNumber, req, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Record " + result.toLowerCase(), result));
    }

    @DeleteMapping("/upload-records/{registerNumber}")
    public ResponseEntity<ApiResponse<Void>> deleteExcelRecord(@PathVariable String registerNumber) {
        excelService.deleteExcelRecord(registerNumber);
        return ResponseEntity.ok(ApiResponse.success("Record deleted", null));
    }
}
