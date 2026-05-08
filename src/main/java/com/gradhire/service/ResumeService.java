package com.gradhire.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.gradhire.dto.ResumeRequestDTO;
import com.gradhire.dto.BulkResumeRequestDTO;
import com.gradhire.entity.ResumeHistory;
import com.gradhire.repository.ResumeHistoryRepository;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ResumeService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ResumeService.class);
    private final StudentService studentService;
    private final ResumeHistoryRepository resumeHistoryRepository;
    private final NotificationService notificationService;
    private final com.gradhire.repository.PlacementRepository placementRepository;
    private final com.gradhire.repository.StudentRepository studentRepository;

    public byte[] generateResumePdf(Long studentId) {
        try {
            com.gradhire.dto.StudentDTO s = studentService.getStudentById(studentId);
            return buildAtsFriendlyPdf(s);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate resume PDF: " + e.getMessage(), e);
        }
    }

    public byte[] generateSingleResumePdf(String regNo, String name) {
        com.gradhire.entity.Student s = studentRepository.findByRegisterNumber(regNo)
                .orElseThrow(() -> new RuntimeException("Student not found for Register Number: " + regNo));
        if (s.getUser() == null || s.getUser().getFullName() == null || !s.getUser().getFullName().equalsIgnoreCase(name.trim())) {
            throw new RuntimeException("Student name and register number do not match");
        }
        try {
            return buildAtsFriendlyPdf(studentService.toDTO(s));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate resume PDF: " + e.getMessage(), e);
        }
    }

    public byte[] generateBulkResumesAsZip(BulkResumeRequestDTO request, String generatedBy) {
        List<com.gradhire.dto.StudentDTO> students = new java.util.ArrayList<>();
        if (request.getRegisterNumbers() != null && !request.getRegisterNumbers().isEmpty()) {
            for (String regNo : request.getRegisterNumbers()) {
                java.util.Optional<com.gradhire.entity.Student> sOpt = studentRepository.findByRegisterNumber(regNo);
                if (sOpt.isPresent()) {
                    students.add(studentService.toDTO(sOpt.get()));
                }
            }
        } else if (request.getBatchId() != null) {
            students = studentService.getStudentsByBatch(request.getBatchId());
        }

        if (students.isEmpty()) {
            throw new RuntimeException("No students found matching the criteria");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (com.gradhire.dto.StudentDTO s : students) {
                try {
                    byte[] pdfBytes = buildAtsFriendlyPdf(s);
                    String regNo = s.getRegisterNumber() != null ? s.getRegisterNumber() : "UNKNOWN";
                    String stuName = s.getFullName() != null ? s.getFullName().replaceAll("\\s+", "_") : "Student";
                    String fileName = regNo + "_" + stuName + "_Resume.pdf";
                    fileName = fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "");

                    ZipEntry entry = new ZipEntry(fileName);
                    zos.putNextEntry(entry);
                    zos.write(pdfBytes);
                    zos.closeEntry();
                } catch (Exception e) {
                    log.error("Skipped resume for student {}: {}", s.getRegisterNumber(), e.getMessage());
                }
            }
            zos.finish();
            
            notificationService.createNotificationByUsername(generatedBy, "Bulk Resume Generation", "Generated " + students.size() + " resumes as ZIP archive", "RESUME_GENERATE");
            
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate bulk resumes zip: " + e.getMessage(), e);
        }
    }

    private byte[] buildAtsFriendlyPdf(com.gradhire.dto.StudentDTO s) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 40, 40);
        PdfWriter.getInstance(doc, output);
        doc.open();

        Font nameFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BaseColor.BLACK);
        Font sectionFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLACK);
        Font textFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);

        // Name
        Paragraph name = new Paragraph(safe(s.getFullName()), nameFont);
        name.setAlignment(Element.ALIGN_CENTER);
        doc.add(name);

        // Contact
        StringBuilder contact = new StringBuilder();
        if (hasVal(s.getEmail())) contact.append(s.getEmail()).append(" | ");
        if (hasVal(s.getPhone())) contact.append(s.getPhone()).append(" | ");
        if (hasVal(s.getLinkedinUrl())) contact.append(s.getLinkedinUrl()).append(" | ");
        if (hasVal(s.getGithubUrl())) contact.append(s.getGithubUrl());
        
        String contactStr = contact.toString();
        if (contactStr.endsWith(" | ")) {
            contactStr = contactStr.substring(0, contactStr.length() - 3);
        }
        Paragraph contactPara = new Paragraph(contactStr, textFont);
        contactPara.setAlignment(Element.ALIGN_CENTER);
        contactPara.setSpacingAfter(15);
        doc.add(contactPara);

        // Education
        addSectionHeader(doc, "Education", sectionFont);
        doc.add(new Paragraph("Degree/Department: " + safe(s.getDepartment()), textFont));
        doc.add(new Paragraph("CGPA: " + safe(s.getCgpa()), textFont));
        doc.add(new Paragraph("UG Percentage: " + safe(s.getUgPercentage()), textFont));
        doc.add(new Paragraph("12th Percentage: " + safe(s.getTwelfthPercent()), textFont));
        doc.add(new Paragraph("10th Percentage: " + safe(s.getTenthPercent()), textFont));
        doc.add(new Paragraph("Batch: " + safe(s.getBatch()), textFont));

        // Skills
        addSectionHeader(doc, "Skills", sectionFont);
        doc.add(new Paragraph(safe(s.getSkills()), textFont));

        // Tools & Technologies
        addSectionHeader(doc, "Tools & Technologies", sectionFont);
        doc.add(new Paragraph(safeList(s.getToolsAndTechnologies()), textFont));

        // Projects
        addSectionHeader(doc, "Projects", sectionFont);
        doc.add(new Paragraph(safeList(s.getProjects()), textFont));

        // Internships
        addSectionHeader(doc, "Internships", sectionFont);
        doc.add(new Paragraph(safeList(s.getInternships()), textFont));

        // Certifications
        addSectionHeader(doc, "Certifications", sectionFont);
        doc.add(new Paragraph(safeList(s.getCertifications()), textFont));

        // Backlogs
        addSectionHeader(doc, "Backlogs", sectionFont);
        doc.add(new Paragraph("Current: " + (s.getCurrentBacklogs() != null ? s.getCurrentBacklogs() : "0"), textFont));
        doc.add(new Paragraph("History: " + (s.getHistoryOfBacklogs() != null ? s.getHistoryOfBacklogs() : "0"), textFont));

        doc.close();
        return output.toByteArray();
    }

    private void addSectionHeader(Document doc, String title, Font font) throws Exception {
        Paragraph p = new Paragraph(title.toUpperCase(), font);
        p.setSpacingBefore(10);
        p.setSpacingAfter(5);
        doc.add(p);
        LineSeparator ls = new LineSeparator();
        ls.setLineColor(BaseColor.BLACK);
        ls.setLineWidth(1f);
        doc.add(new Chunk(ls));
        doc.add(Chunk.NEWLINE);
    }

    private boolean hasVal(Object obj) {
        return obj != null && !obj.toString().trim().isEmpty();
    }

    private String safe(Object obj) {
        if (obj == null) return "N/A";
        String str = obj.toString().trim();
        return str.isEmpty() ? "N/A" : str;
    }

    private String safeList(List<String> list) {
        if (list == null || list.isEmpty()) return "N/A";
        return String.join(", ", list);
    }

    // Retained for backward compatibility
    public List<com.gradhire.dto.StudentDTO> filterStudents(ResumeRequestDTO request) {
        List<com.gradhire.dto.StudentDTO> students = studentService.getAllStudents();
        
        if (request.getCgpa() != null) {
            students.removeIf(s -> s.getCgpa() == null || s.getCgpa() < request.getCgpa());
        }
        if (request.getTenthPercent() != null && !request.getTenthPercent().toString().isBlank()) {
            try {
                double min10 = Double.parseDouble(request.getTenthPercent().toString().trim().replace("%", ""));
                students.removeIf(s -> {
                    if (s.getTenthPercent() == null || s.getTenthPercent().isBlank()) return true;
                    try { return Double.parseDouble(s.getTenthPercent().trim().replace("%", "")) < min10; } catch (Exception e) { return true; }
                });
            } catch (NumberFormatException ignored) {}
        }
        if (request.getTwelfthPercent() != null && !request.getTwelfthPercent().toString().isBlank()) {
            try {
                double min12 = Double.parseDouble(request.getTwelfthPercent().toString().trim().replace("%", ""));
                students.removeIf(s -> {
                    if (s.getTwelfthPercent() == null || s.getTwelfthPercent().isBlank()) return true;
                    try { return Double.parseDouble(s.getTwelfthPercent().trim().replace("%", "")) < min12; } catch (Exception e) { return true; }
                });
            } catch (NumberFormatException ignored) {}
        }
        if (request.getSkills() != null && !request.getSkills().isBlank()) {
            String[] reqSkillsArr = request.getSkills().toLowerCase().split("\\s*,\\s*");
            students.removeIf(s -> {
                if (s.getSkills() == null || s.getSkills().isBlank()) return true;
                String studentSkills = s.getSkills().toLowerCase().trim();
                for (String skill : reqSkillsArr) {
                    if (!skill.isBlank() && !studentSkills.contains(skill.trim())) {
                        return true;
                    }
                }
                return false;
            });
        }
        if (request.getDomain() != null && !request.getDomain().isBlank()) {
            String reqDomain = request.getDomain().toLowerCase().trim();
            students.removeIf(s -> s.getDomain() == null || !s.getDomain().toLowerCase().trim().equals(reqDomain));
        }
        if (request.getDepartments() != null && !request.getDepartments().isEmpty()) {
            List<String> deptLower = request.getDepartments().stream().map(String::toLowerCase).map(String::trim).filter(d -> !d.isBlank()).toList();
            if (!deptLower.isEmpty()) {
                students.removeIf(s -> {
                    if (s.getDepartment() == null) return true;
                    String sDept = s.getDepartment().toLowerCase().trim();
                    return !deptLower.contains(sDept);
                });
            }
        }
        if (request.getBatchYears() != null && !request.getBatchYears().isEmpty()) {
            students.removeIf(s -> {
                if (s.getBatchYear() != null && request.getBatchYears().contains(s.getBatchYear())) return false;
                if (s.getBatch() != null) {
                    String bv = s.getBatch().trim();
                    if (bv.endsWith(".0")) bv = bv.substring(0, bv.length() - 2);
                    try {
                        Integer by = Integer.parseInt(bv);
                        if (request.getBatchYears().contains(by)) return false;
                    } catch (NumberFormatException ignored) {}
                    for (Integer by : request.getBatchYears()) {
                        if (bv.equals(String.valueOf(by))) return false;
                    }
                }
                return true;
            });
        }
        if (request.getCurrentBacklogs() != null) {
            students.removeIf(s -> s.getCurrentBacklogs() == null || s.getCurrentBacklogs() > request.getCurrentBacklogs());
        }
        if (request.getHistoryOfBacklogs() != null) {
            students.removeIf(s -> s.getHistoryOfBacklogs() == null || s.getHistoryOfBacklogs() > request.getHistoryOfBacklogs());
        }
        return students;
    }

    public ResumeHistory generateBulkResumes(ResumeRequestDTO request, String generatedBy) {
        try {
            List<com.gradhire.dto.StudentDTO> students = filterStudents(request);
            log.info("Students remaining after applying all filters: {}", students.size());

            if (students.isEmpty()) {
                return null;
            }

            java.io.File uploadDir = new java.io.File("uploads/resumes");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            String uniqueFileName = "Resumes_" + System.currentTimeMillis() + ".zip";
            java.io.File zipFile = new java.io.File(uploadDir, uniqueFileName);

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                for (com.gradhire.dto.StudentDTO s : students) {
                    try {
                        byte[] pdfBytes = generateCompactResumePdf(s);
                        String regNo = s.getRegisterNumber() != null ? s.getRegisterNumber() : "UNKNOWN";
                        String stuName = s.getFullName() != null ? s.getFullName().replaceAll("\\s+", "_") : "Student";
                        String fileName = regNo + "_" + stuName + "_Resume.pdf";
                        fileName = fileName.replaceAll("[^a-zA-Z0-9.\\-_]", "");
                        
                        ZipEntry entry = new ZipEntry(fileName);
                        zos.putNextEntry(entry);
                        zos.write(pdfBytes);
                        zos.closeEntry();
                    } catch (Exception e) {
                        log.error("Skipped resume for student {}: {}", s.getRegisterNumber(), e.getMessage());
                    }
                }
            }

            ResumeHistory history = ResumeHistory.builder()
                    .fileName(uniqueFileName)
                    .filePath(zipFile.getAbsolutePath())
                    .generatedBy(generatedBy)
                    .build();

            ResumeHistory saved = resumeHistoryRepository.save(history);
            
            notificationService.createNotificationByUsername(generatedBy, "Bulk Resume Generation", "Generated " + students.size() + " resumes", "RESUME_GENERATE");

            return saved;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate bulk resumes: " + e.getMessage(), e);
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 * * ?")
    @org.springframework.transaction.annotation.Transactional
    public void cleanupOldResumes() {
        List<ResumeHistory> oldHistory = resumeHistoryRepository.findByExpirationDateBefore(java.time.LocalDateTime.now());
        for (ResumeHistory h : oldHistory) {
            if (h.getFilePath() != null) {
                java.io.File f = new java.io.File(h.getFilePath());
                if (f.exists()) f.delete();
            }
        }
        resumeHistoryRepository.deleteByExpirationDateBefore(java.time.LocalDateTime.now());
        log.info("Cleaned up expired resumes.");
    }

    public byte[] generateCompactResumePdf(com.gradhire.dto.StudentDTO s) {
        try {
            return buildAtsFriendlyPdf(s);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate compact resume: " + e.getMessage(), e);
        }
    }
}
