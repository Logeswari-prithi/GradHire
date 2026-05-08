package com.gradhire.controller;

import com.gradhire.dto.ApiResponse;
import com.gradhire.entity.PlacementDrive;
import com.gradhire.entity.Placement;
import com.gradhire.entity.PlacementStatus;
import com.gradhire.entity.User;
import com.gradhire.repository.UserRepository;
import com.gradhire.repository.StudentRepository;
import com.gradhire.repository.PlacementRepository;
import com.gradhire.service.PlacementDriveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/placement-drives")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PlacementDriveController {

    private final PlacementDriveService driveService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PlacementRepository placementRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<PlacementDrive>>> getAllDrives() {
        return ResponseEntity.ok(ApiResponse.success("Drives retrieved", driveService.getAllDrives()));
    }

    @GetMapping("/ongoing")
    public ResponseEntity<ApiResponse<java.util.List<PlacementDrive>>> getOngoingDrives() {
        return ResponseEntity.ok(ApiResponse.success("Ongoing drives retrieved", driveService.getOngoingDrives()));
    }

    @GetMapping("/past")
    public ResponseEntity<ApiResponse<java.util.List<PlacementDrive>>> getPastDrives() {
        return ResponseEntity.ok(ApiResponse.success("Past drives retrieved", driveService.getPastDrives()));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<PlacementDrive>> createDrive(@RequestBody PlacementDrive drive) {
        try {
            if (drive.getCompanyName() == null || drive.getCompanyName().isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Company name is required"));
            }
            return ResponseEntity.ok(ApiResponse.success("Drive created", driveService.createDrive(drive)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create drive: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<PlacementDrive>> updateDrive(@PathVariable Long id, @RequestBody PlacementDrive drive) {
        return ResponseEntity.ok(ApiResponse.success("Drive updated", driveService.updateDrive(id, drive)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDrive(@PathVariable Long id) {
        driveService.deleteDrive(id);
        return ResponseEntity.ok(ApiResponse.success("Drive deleted", null));
    }

    @PostMapping("/{driveId}/apply")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> applyForDrive(@PathVariable Long driveId, org.springframework.security.core.Authentication auth) {
        try {
            User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            com.gradhire.entity.Student student = studentRepository.findByUserId(user.getId()).orElseThrow(() -> new RuntimeException("Student profile not found"));
            
            PlacementDrive drive = driveService.getAllDrives().stream().filter(d -> d.getId().equals(driveId)).findFirst()
                    .orElseThrow(() -> new RuntimeException("Drive not found"));
            
            // Check if already applied
            boolean alreadyApplied = placementRepository.findByStudentId(student.getId()).stream()
                    .anyMatch(p -> p.getDrive() != null && p.getDrive().getId().equals(driveId));
            if (alreadyApplied) {
                return ResponseEntity.badRequest().body(ApiResponse.error("You have already applied for this drive"));
            }
            
            Placement p = new Placement();
            p.setStudent(student);
            p.setDrive(drive);
            p.setCompanyName(drive.getCompanyName());
            p.setJobRole(drive.getJobRole() != null ? drive.getJobRole() : "Candidate");
            p.setPackageOffered(drive.getPackageOffered());
            p.setOverallStatus(PlacementStatus.APPLIED);
            
            placementRepository.save(p);
            
            return ResponseEntity.ok(ApiResponse.success("Successfully applied for " + drive.getCompanyName(), null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
