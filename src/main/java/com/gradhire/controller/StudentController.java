package com.gradhire.controller;

import com.gradhire.dto.*;
import com.gradhire.repository.BatchRepository;
import com.gradhire.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final BatchRepository batchRepository;
    private final com.gradhire.service.NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Students retrieved", studentService.getAllStudents()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Student found", studentService.getStudentById(id)));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<ApiResponse<StudentDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Student found", studentService.getStudentByUserId(userId)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success("Results", studentService.searchStudents(q)));
    }

    @GetMapping("/batch/{batchId}")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getByBatch(@PathVariable Long batchId) {
        return ResponseEntity.ok(ApiResponse.success("Students retrieved",
                studentService.getStudentsByBatch(batchId)));
    }

    @GetMapping("/department/{dept}")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getByDepartment(@PathVariable String dept) {
        return ResponseEntity.ok(ApiResponse.success("Students retrieved",
                studentService.getStudentsByDepartment(dept)));
    }

    @GetMapping("/upload/{uploadId}")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getByUpload(@PathVariable Long uploadId) {
        try {
            List<StudentDTO> students = studentService.getStudentsByUpload(uploadId);
            if (students.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success("No students found for this upload", students));
            }
            return ResponseEntity.ok(ApiResponse.success("Students retrieved", students));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to load upload data: " + e.getMessage()));
        }
    }

    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<String>>> getDepartments() {
        return ResponseEntity.ok(ApiResponse.success("Departments", studentService.getDepartments()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<StudentDTO>> updateProfile(@RequestBody StudentDTO dto,
                                                                   Authentication auth) {
        try {
            String username = auth.getName();
            StudentDTO student = studentService.getStudentByUsername(username);
            return ResponseEntity.ok(ApiResponse.success("Profile updated",
                    studentService.updateStudent(student.getId(), dto, username)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to save profile: " + e.getMessage()));
        }
    }

    @PostMapping("/photo")
    public ResponseEntity<ApiResponse<String>> uploadPhoto(@RequestParam("file") MultipartFile file, Authentication auth) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File is empty"));
            }

            String username = auth.getName();
            StudentDTO studentDTO = studentService.getStudentByUsername(username);
            
            // Ensure directory exists
            Path uploadDir = Paths.get("uploads", "photos");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate filename based on student reg no or id to prevent overriding if not intended, or just override. Let's use register number + extension
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
            
            String filename = (studentDTO.getRegisterNumber() != null ? studentDTO.getRegisterNumber() : "student_" + studentDTO.getId()) + extension;
            Path filePath = uploadDir.resolve(filename);
            
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Generate a URL for the photo (relative or absolute based on how static resources are served. We can serve it through an endpoint or add a resource handler)
            // For now, we'll store the relative API path
            String photoUrl = "/api/student/photo/" + filename;
            
            studentDTO.setProfilePhotoUrl(photoUrl);
            studentService.updateStudent(studentDTO.getId(), studentDTO, username);

            return ResponseEntity.ok(ApiResponse.success("Photo uploaded successfully", photoUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to upload photo: " + e.getMessage()));
        }
    }

    @GetMapping("/photo/{filename}")
    public ResponseEntity<org.springframework.core.io.Resource> getPhoto(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads", "photos").resolve(filename).normalize();
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(org.springframework.http.MediaType.IMAGE_JPEG) // simplistic, could use a utility to detect mime type
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentDTO>> update(@PathVariable Long id,
                                                           @RequestBody StudentDTO dto,
                                                           Authentication auth) {
        try {
            String username = auth != null ? auth.getName() : "System";
            return ResponseEntity.ok(ApiResponse.success("Student updated",
                    studentService.updateStudent(id, dto, username)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Update failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication auth) {
        String username = auth != null ? auth.getName() : "System";
        studentService.deleteStudent(id, username);
        return ResponseEntity.ok(ApiResponse.success("Student deleted", null));
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<com.gradhire.entity.Batch>>> getBatches() {
        return ResponseEntity.ok(ApiResponse.success("Batches retrieved", batchRepository.findAll()));
    }

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<com.gradhire.entity.Notification>>> getNotifications(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", 
                notificationService.getNotificationsForUser(auth.getName())));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success("Unread count", 
                notificationService.getUnreadCountForUser(auth.getName())));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }
}
