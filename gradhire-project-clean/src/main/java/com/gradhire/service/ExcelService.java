package com.gradhire.service;

import com.gradhire.dto.CreateUserRequest;
import com.gradhire.entity.UploadHistory;
import com.gradhire.exception.BadRequestException;
import com.gradhire.repository.UploadHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExcelService.class);
    private final UserService userService;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final NotificationService notificationService;
    private final com.gradhire.repository.StudentRepository studentRepository;

    // Common header name normalizations
    private static final Map<String, String> HEADER_MAP = new LinkedHashMap<>();
    static {
        HEADER_MAP.put("register", "registerNumber");
        HEADER_MAP.put("reg", "registerNumber");
        HEADER_MAP.put("rollno", "registerNumber");
        HEADER_MAP.put("roll", "registerNumber");
        HEADER_MAP.put("regno", "registerNumber");
        HEADER_MAP.put("registernumber", "registerNumber");
        HEADER_MAP.put("name", "fullName");
        HEADER_MAP.put("fullname", "fullName");
        HEADER_MAP.put("studentname", "fullName");
        HEADER_MAP.put("email", "email");
        HEADER_MAP.put("emailid", "email");
        HEADER_MAP.put("department", "department");
        HEADER_MAP.put("dept", "department");
        HEADER_MAP.put("branch", "department");
        HEADER_MAP.put("batch", "batchYear");
        HEADER_MAP.put("batchyear", "batchYear");
        HEADER_MAP.put("year", "batchYear");
        HEADER_MAP.put("phone", "phone");
        HEADER_MAP.put("phoneno", "phone");
        HEADER_MAP.put("mobile", "phone");
        HEADER_MAP.put("mobileno", "phone");
        HEADER_MAP.put("contact", "phone");
        HEADER_MAP.put("cgpa", "cgpa");
        HEADER_MAP.put("gpa", "cgpa");
        HEADER_MAP.put("gender", "gender");
        HEADER_MAP.put("sex", "gender");
        HEADER_MAP.put("10th", "tenthPercent");
        HEADER_MAP.put("tenth", "tenthPercent");
        HEADER_MAP.put("tenthpercent", "tenthPercent");
        HEADER_MAP.put("10thmarks", "tenthPercent");
        HEADER_MAP.put("sslc", "tenthPercent");
        HEADER_MAP.put("12th", "twelfthPercent");
        HEADER_MAP.put("twelfth", "twelfthPercent");
        HEADER_MAP.put("twelfthpercent", "twelfthPercent");
        HEADER_MAP.put("12thmarks", "twelfthPercent");
        HEADER_MAP.put("hsc", "twelfthPercent");
        HEADER_MAP.put("ug", "ugPercentage");
        HEADER_MAP.put("ugdetails", "ugPercentage");
        HEADER_MAP.put("ugpercentage", "ugPercentage");
        HEADER_MAP.put("degree", "ugPercentage");
        HEADER_MAP.put("skills", "skills");
        HEADER_MAP.put("skill", "skills");
        HEADER_MAP.put("tools", "tools");
        HEADER_MAP.put("toolsandtechnologies", "tools");
        HEADER_MAP.put("technologies", "tools");
        HEADER_MAP.put("domain", "domain");
        HEADER_MAP.put("specialization", "domain");
        HEADER_MAP.put("placementstatus", "placementStatus");
        HEADER_MAP.put("status", "placementStatus");
        HEADER_MAP.put("currentbacklogs", "currentBacklogs");
        HEADER_MAP.put("backlogs", "currentBacklogs");
        HEADER_MAP.put("activebacklog", "currentBacklogs");
        HEADER_MAP.put("historyofbacklogs", "historyOfBacklogs");
        HEADER_MAP.put("pastbacklogs", "historyOfBacklogs");
        HEADER_MAP.put("careergap", "careerGap");
        HEADER_MAP.put("gap", "careerGap");
        HEADER_MAP.put("internship", "internshipExperience");
        HEADER_MAP.put("internships", "internshipExperience");
        HEADER_MAP.put("projects", "projectDetails");
        HEADER_MAP.put("project", "projectDetails");
        HEADER_MAP.put("certifications", "certifications");
        HEADER_MAP.put("address", "address");
        HEADER_MAP.put("dob", "dob");
        HEADER_MAP.put("linkedin", "linkedinUrl");
        HEADER_MAP.put("github", "githubUrl");
        HEADER_MAP.put("languages", "languages");
    }

    public Map<String, Object> uploadStudentsFromExcel(MultipartFile file, Integer batchYear, String uploadedBy) {
        List<String> results = new ArrayList<>();
        int successCount = 0, updateCount = 0, failedCount = 0, totalRecords = 0;

        // Create upload history record
        UploadHistory history = UploadHistory.builder()
                .fileName(file.getOriginalFilename())
                .uploadedBy(uploadedBy)
                .batchYear(batchYear)
                .status("PROCESSING")
                .build();
        uploadHistoryRepository.save(history);

        try (Workbook workbook = createWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Parse header row
            Map<Integer, String> columnMapping = new HashMap<>();
            if (rows.hasNext()) {
                Row headerRow = rows.next();
                for (Cell cell : headerRow) {
                    String headerText = cellValue(cell).toLowerCase().replaceAll("[^a-z0-9]", "");
                    String mappedField = HEADER_MAP.get(headerText);
                    if (mappedField != null) {
                        columnMapping.put(cell.getColumnIndex(), mappedField);
                    } else {
                        log.warn("Unknown Excel column header: '{}' at column {}", cellValue(cell), cell.getColumnIndex());
                    }
                }
            }

            if (!columnMapping.containsValue("registerNumber")) {
                throw new BadRequestException("Excel must have a Register Number column (RegisterNo, RegNo, Roll No, etc.)");
            }

            int rowNum = 2;
            while (rows.hasNext()) {
                Row row = rows.next();
                totalRecords++;
                try {
                    Map<String, String> rowData = new HashMap<>();
                    for (Map.Entry<Integer, String> entry : columnMapping.entrySet()) {
                        String val = cellValue(row.getCell(entry.getKey()));
                        if (!val.isBlank()) {
                            rowData.put(entry.getValue(), val.trim());
                        }
                    }

                    String regNo = rowData.get("registerNumber");
                    if (regNo == null || regNo.isBlank()) {
                        results.add("Row " + rowNum + ": Skipped - Register number is empty");
                        failedCount++;
                        rowNum++; continue;
                    }

                    CreateUserRequest req = new CreateUserRequest();
                    req.setRegisterNumber(regNo);
                    req.setFullName(rowData.getOrDefault("fullName", regNo));
                    req.setEmail(rowData.get("email"));
                    req.setDepartment(rowData.get("department"));
                    req.setRole("STUDENT");
                    req.setPhone(rowData.get("phone"));
                    req.setGender(rowData.get("gender"));
                    req.setTenthPercent(rowData.get("tenthPercent"));
                    req.setTwelfthPercent(rowData.get("twelfthPercent"));
                    req.setUgPercentage(rowData.get("ugPercentage"));
                    req.setSkills(rowData.get("skills"));
                    req.setToolsAndTechnologies(toList(rowData.get("tools")));
                    req.setDomain(rowData.get("domain"));
                    req.setAddress(rowData.get("address"));
                    req.setPlacementStatus(rowData.get("placementStatus"));
                    req.setStatus(rowData.get("placementStatus"));
                    req.setCareerGap(rowData.get("careerGap"));
                    req.setInternships(toList(rowData.get("internshipExperience")));
                    req.setProjects(toList(rowData.get("projectDetails")));
                    req.setCertifications(toList(rowData.get("certifications")));
                    req.setLanguages(rowData.get("languages"));
                    req.setLinkedinUrl(rowData.get("linkedinUrl"));
                    req.setGithubUrl(rowData.get("githubUrl"));
                    if(rowData.get("dob") != null) {
                        try { req.setDob(java.time.LocalDate.parse(rowData.get("dob"))); } catch(Exception ignored) {}
                    }

                    // Parse numeric fields safely
                    String cgpaStr = rowData.get("cgpa");
                    if (cgpaStr != null && !cgpaStr.isBlank()) {
                        try { 
                            String cleanCgpa = cgpaStr.replaceAll("[^0-9.]", "");
                            if(!cleanCgpa.isEmpty()) {
                                req.setCgpa(Double.parseDouble(cleanCgpa)); 
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                    if (batchYear != null) {
                        req.setBatchYear(batchYear);
                        req.setBatch(String.valueOf(batchYear));
                    } else {
                        String batchStr = rowData.get("batchYear");
                        if (batchStr != null && !batchStr.isBlank()) {
                            try { 
                                String cleanBatch = batchStr.split("\\.")[0].replaceAll("[^0-9]", "");
                                if(!cleanBatch.isEmpty()) {
                                    req.setBatchYear(Integer.parseInt(cleanBatch)); 
                                }
                            } catch (NumberFormatException ignored) {}
                            req.setBatch(batchStr.split("\\.")[0].trim());
                        }
                    }
                    String curBacklogs = rowData.get("currentBacklogs");
                    if (curBacklogs != null) {
                        try { req.setCurrentBacklogs(Integer.parseInt(curBacklogs.split("\\.")[0].replaceAll("[^0-9]", ""))); } catch (NumberFormatException ignored) {}
                    }
                    String histBacklogs = rowData.get("historyOfBacklogs");
                    if (histBacklogs != null) {
                        try { req.setHistoryOfBacklogs(Integer.parseInt(histBacklogs.split("\\.")[0].replaceAll("[^0-9]", ""))); } catch (NumberFormatException ignored) {}
                    }

                    String result = userService.createOrUpdateStudentFromExcel(req, uploadedBy, history.getId());
                    if ("UPDATED".equals(result)) {
                        results.add("Row " + rowNum + ": " + req.getFullName() + " updated successfully");
                        updateCount++;
                    } else {
                        results.add("Row " + rowNum + ": " + req.getFullName() + " created successfully");
                        successCount++;
                    }
                } catch (Exception e) {
                    results.add("Row " + rowNum + ": ERROR - " + e.getMessage());
                    failedCount++;
                }
                rowNum++;
            }
        } catch (IOException e) {
            throw new BadRequestException("Cannot read file: " + e.getMessage());
        }

        // Update history
        history.setTotalRecords(totalRecords);
        history.setSuccessCount(successCount);
        history.setUpdateCount(updateCount);
        history.setFailedCount(failedCount);
        history.setStatus(failedCount == 0 ? "SUCCESS" : (successCount + updateCount > 0 ? "PARTIAL" : "FAILED"));
        uploadHistoryRepository.save(history);

        notificationService.createNotificationByUsername(uploadedBy, "Excel Upload Completed", "Processed " + totalRecords + " students. Success: " + successCount + ", Updated: " + updateCount, "EXCEL_UPLOAD");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        response.put("uploadId", history.getId());
        response.put("totalRecords", totalRecords);
        response.put("successCount", successCount);
        response.put("updateCount", updateCount);
        response.put("failedCount", failedCount);
        return response;
    }

    private Workbook createWorkbook(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name != null && name.endsWith(".xls")) {
            return new HSSFWorkbook(file.getInputStream());
        }
        return new XSSFWorkbook(file.getInputStream());
    }

    private List<String> toList(String val) {
        if (val == null || val.isBlank()) return new ArrayList<>();
        return Arrays.asList(val.split("\\s*,\\s*"));
    }

    private String cellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    yield String.valueOf((long) d);
                }
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue().trim(); }
                catch (Exception e) {
                    try { yield String.valueOf(cell.getNumericCellValue()); }
                    catch (Exception e2) { yield ""; }
                }
            }
            default -> "";
        };
    }

    public List<UploadHistory> getUploadHistory() {
        return uploadHistoryRepository.findAllByOrderByUploadDateDesc();
    }

    public void deleteUploadHistory(Long id) {
        uploadHistoryRepository.deleteById(id);
    }

    @org.springframework.transaction.annotation.Transactional
    public String updateExcelRecord(String registerNumber, CreateUserRequest request, String modifiedBy) {
        // Find existing student by regNo
        var existingStudent = studentRepository.findByRegisterNumber(registerNumber)
            .orElseThrow(() -> new com.gradhire.exception.ResourceNotFoundException("Record not found for Register Number: " + registerNumber));
        
        // Ensure request regNo matches
        request.setRegisterNumber(registerNumber);
        
        // We reuse the UserService logic which correctly UPDATES based on finding the student
        return userService.createOrUpdateStudentFromExcel(request, modifiedBy, existingStudent.getUploadHistoryId());
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteExcelRecord(String registerNumber) {
        var student = studentRepository.findByRegisterNumber(registerNumber)
            .orElseThrow(() -> new com.gradhire.exception.ResourceNotFoundException("Record not found for Register Number: " + registerNumber));
        studentRepository.delete(student);
    }
}
