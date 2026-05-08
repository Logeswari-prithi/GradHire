package com.gradhire.controller;

import com.gradhire.dto.BulkResumeRequestDTO;
import com.gradhire.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(value = "/bulk", produces = "application/zip")
    public ResponseEntity<byte[]> generateBulkResume(@RequestBody BulkResumeRequestDTO request, Authentication auth) {
        String generatedBy = auth != null ? auth.getName() : "System";
        byte[] zipBytes = resumeService.generateBulkResumesAsZip(request, generatedBy);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Bulk_Resumes_" + System.currentTimeMillis() + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zipBytes);
    }

    @GetMapping(value = "/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadSingleResume(@RequestParam String regNo, @RequestParam String name) {
        byte[] pdfBytes = resumeService.generateSingleResumePdf(regNo, name);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + regNo + "_Resume.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
