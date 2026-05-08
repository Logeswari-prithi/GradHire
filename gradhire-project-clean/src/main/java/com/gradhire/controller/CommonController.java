package com.gradhire.controller;

import com.gradhire.dto.ApiResponse;
import com.gradhire.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/common")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CommonController {

    private final NotificationService notificationService;

    private final com.gradhire.repository.UserRepository userRepository;

    /**
     * Any authenticated user (STUDENT, STAFF, ADMIN) can submit an error report
     */
    @PostMapping("/error-report")
    public ResponseEntity<ApiResponse<Void>> reportError(
            @RequestParam String subject,
            @RequestParam String description,
            @RequestParam(defaultValue = "LOW") String severity,
            Authentication auth) {
        
        com.gradhire.entity.User user = userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        notificationService.submitErrorReport(user.getId(), subject, description, severity);
        return ResponseEntity.ok(ApiResponse.success("Error report submitted", null));
    }
}
