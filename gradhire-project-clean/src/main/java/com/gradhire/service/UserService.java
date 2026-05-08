package com.gradhire.service;

import com.gradhire.dto.CreateUserRequest;
import com.gradhire.dto.RegisterRequest;
import com.gradhire.entity.*;
import com.gradhire.entity.Role;
import com.gradhire.exception.BadRequestException;
import com.gradhire.exception.DuplicateResourceException;
import com.gradhire.exception.ResourceNotFoundException;
import com.gradhire.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final BatchRepository batchRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional
    public User createUser(CreateUserRequest request, String createdBy) {
        Role role = Role.valueOf(request.getRole());
        String username = (role == Role.STUDENT) ? request.getRegisterNumber() :
                          (request.getUsername() != null && !request.getUsername().isBlank() ? request.getUsername() : request.getEmail());

        if (username == null || username.isBlank()) {
            throw new BadRequestException("Username is required");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username/Register number already exists: " + username);
        }

        User user = User.builder()
                .username(username)
                .email(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail() : username + "@gradhire.com")
                .fullName(request.getFullName())
                .role(role)
                .password(passwordEncoder.encode(role == Role.ADMIN ? "Admin@123" : "Admin@1234"))
                .phone(request.getPhone())
                .department(request.getDepartment())
                .enabled(true)
                .passwordChanged(false)
                .forcePasswordChange(true)
                .modifiedBy(createdBy)
                .build();
        userRepository.save(user);

        if (role == Role.STUDENT) {
            if (request.getRegisterNumber() == null || request.getRegisterNumber().isBlank()) {
                throw new BadRequestException("Register number is required for student");
            }
            if (studentRepository.existsByRegisterNumber(request.getRegisterNumber())) {
                throw new DuplicateResourceException("Register number already exists: " + request.getRegisterNumber());
            }
            Batch batch = null;
            if (request.getBatchYear() != null) {
                batch = batchRepository.findByYear(request.getBatchYear())
                        .orElseGet(() -> batchRepository.save(
                                Batch.builder().year(request.getBatchYear())
                                        .description("Batch of " + request.getBatchYear()).build()));
            }
            Student student = Student.builder()
                    .user(user)
                    .registerNumber(request.getRegisterNumber())
                    .department(request.getDepartment())
                    .batch(batch)
                    .cgpa(request.getCgpa())
                    .gender(request.getGender())
                    .tenthPercent(request.getTenthPercent())
                    .twelfthPercent(request.getTwelfthPercent())
                    .ugPercentage(request.getUgPercentage())
                    .skills(request.getSkills())
                    .domain(request.getDomain())
                    .address(request.getAddress())
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .currentBacklogs(request.getCurrentBacklogs())
                    .historyOfBacklogs(request.getHistoryOfBacklogs())
                    .careerGap(request.getCareerGap())
                    .internships(request.getInternships())
                    .projects(request.getProjects())
                    .certifications(request.getCertifications())
                    .languages(request.getLanguages())
                    .dob(request.getDob())
                    .linkedinUrl(request.getLinkedinUrl())
                    .githubUrl(request.getGithubUrl())
                    .placementStatus(request.getPlacementStatus())
                    .modifiedBy(createdBy)
                    .build();
            studentRepository.save(student);

        } else if (role == Role.STAFF) {
            staffRepository.save(Staff.builder().user(user).department(request.getDepartment()).build());
        }
        
        notificationService.createNotificationByUsername(createdBy, "User Created", "Created " + role.name() + " user: " + user.getUsername(), "USER_ADD");
        return user;
    }

    /**
     * Create or update student from Excel upload. Returns "CREATED", "UPDATED", or throws.
     */
    @Transactional
    public String createOrUpdateStudentFromExcel(CreateUserRequest request, String uploadedBy, Long uploadHistoryId) {
        String regNo = request.getRegisterNumber().trim();

        // ── Normalize data before saving to DB ──
        // Skills: trim each skill, lowercase-normalize for consistent matching
        if (request.getSkills() != null) {
            String sanitizedSkills = java.util.Arrays.stream(request.getSkills().split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.joining(", "));
            request.setSkills(sanitizedSkills);
        }
        // 10th/12th: strip %, trim spaces, keep only the numeric value
        if (request.getTenthPercent() != null) {
            request.setTenthPercent(normalizePercent(request.getTenthPercent()));
        }
        if (request.getTwelfthPercent() != null) {
            request.setTwelfthPercent(normalizePercent(request.getTwelfthPercent()));
        }
        // Department: trim
        if (request.getDepartment() != null) {
            request.setDepartment(request.getDepartment().trim());
        }
        // Domain: trim
        if (request.getDomain() != null) {
            request.setDomain(request.getDomain().trim());
        }
        // Batch value: strip decimal (.0) that Excel numeric cells produce
        if (request.getBatch() != null) {
            String bv = request.getBatch().trim();
            if (bv.endsWith(".0")) bv = bv.substring(0, bv.length() - 2);
            request.setBatch(bv);
        }

        // Check if student already exists
        var existingStudent = studentRepository.findByRegisterNumber(regNo);
        if (existingStudent.isPresent()) {
            // UPDATE existing student profile
            Student student = existingStudent.get();
            if (request.getFullName() != null && !request.getFullName().isBlank()) {
                student.getUser().setFullName(request.getFullName());
            }
            if (request.getDepartment() != null && !request.getDepartment().isBlank()) {
                student.setDepartment(request.getDepartment());
                student.getUser().setDepartment(request.getDepartment());
            }
            if (request.getEmail() != null && !request.getEmail().isBlank()) {
                student.setEmail(request.getEmail());
                // Only update user email if not already taken by another user
                if (student.getUser().getEmail() == null || student.getUser().getEmail().isBlank()) {
                    student.getUser().setEmail(request.getEmail());
                }
            }
            if (request.getPhone() != null && !request.getPhone().isBlank()) {
                student.setPhone(request.getPhone());
                student.getUser().setPhone(request.getPhone());
            }
            if (request.getCgpa() != null) student.setCgpa(request.getCgpa());
            if (request.getGender() != null) student.setGender(request.getGender());
            if (request.getTenthPercent() != null) student.setTenthPercent(request.getTenthPercent());
            if (request.getTwelfthPercent() != null) student.setTwelfthPercent(request.getTwelfthPercent());
            if (request.getUgPercentage() != null) student.setUgPercentage(request.getUgPercentage());
            if (request.getSkills() != null) student.setSkills(request.getSkills());
            if (request.getDomain() != null) student.setDomain(request.getDomain());
            if (request.getAddress() != null) student.setAddress(request.getAddress());
            if (request.getPlacementStatus() != null) student.setPlacementStatus(request.getPlacementStatus());
            if (request.getStatus() != null) student.setStatus(request.getStatus());
            if (request.getCurrentBacklogs() != null) student.setCurrentBacklogs(request.getCurrentBacklogs());
            if (request.getHistoryOfBacklogs() != null) student.setHistoryOfBacklogs(request.getHistoryOfBacklogs());
            if (request.getCareerGap() != null) student.setCareerGap(request.getCareerGap());
            if (request.getInternships() != null) student.setInternships(request.getInternships());
            if (request.getProjects() != null) student.setProjects(request.getProjects());
            if (request.getCertifications() != null) student.setCertifications(request.getCertifications());
            if (request.getLanguages() != null) student.setLanguages(request.getLanguages());
            if (request.getDob() != null) student.setDob(request.getDob());
            if (request.getLinkedinUrl() != null) student.setLinkedinUrl(request.getLinkedinUrl());
            if (request.getGithubUrl() != null) student.setGithubUrl(request.getGithubUrl());
            if (request.getBatch() != null) student.setBatchValue(request.getBatch());
            if (request.getBatchYear() != null) {
                Batch batch = batchRepository.findByYear(request.getBatchYear())
                        .orElseGet(() -> batchRepository.save(
                                Batch.builder().year(request.getBatchYear())
                                        .description("Batch of " + request.getBatchYear()).build()));
                student.setBatch(batch);
            }
            student.setUploadHistoryId(uploadHistoryId);
            student.setModifiedBy(uploadedBy);
            userRepository.save(student.getUser());
            studentRepository.save(student);
            return "UPDATED";
        } else {
            // CREATE new student + user
            String username = regNo;
            if (userRepository.existsByUsername(username)) {
                // User exists but no student record - link them
                User existingUser = userRepository.findByUsername(username)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
                Batch batch = null;
                if (request.getBatchYear() != null) {
                    batch = batchRepository.findByYear(request.getBatchYear())
                            .orElseGet(() -> batchRepository.save(
                                    Batch.builder().year(request.getBatchYear())
                                            .description("Batch of " + request.getBatchYear()).build()));
                }
                Student student = Student.builder()
                        .user(existingUser)
                        .registerNumber(regNo)
                        .department(request.getDepartment())
                        .batch(batch)
                        .cgpa(request.getCgpa())
                        .gender(request.getGender())
                        .tenthPercent(request.getTenthPercent())
                        .twelfthPercent(request.getTwelfthPercent())
                        .ugPercentage(request.getUgPercentage())
                        .skills(request.getSkills())
                        .domain(request.getDomain())
                        .address(request.getAddress())
                        .phone(request.getPhone())
                        .email(request.getEmail())
                        .placementStatus(request.getPlacementStatus())
                        .status(request.getStatus())
                        .currentBacklogs(request.getCurrentBacklogs())
                        .historyOfBacklogs(request.getHistoryOfBacklogs())
                        .careerGap(request.getCareerGap())
                        .internships(request.getInternships())
                        .projects(request.getProjects())
                        .certifications(request.getCertifications())
                        .languages(request.getLanguages())
                        .dob(request.getDob())
                        .linkedinUrl(request.getLinkedinUrl())
                        .githubUrl(request.getGithubUrl())
                        .batchValue(request.getBatch())
                        .uploadHistoryId(uploadHistoryId)
                        .modifiedBy(uploadedBy)
                        .build();
                studentRepository.save(student);
                return "CREATED";
            }

            // Create brand new user + student
            User user = User.builder()
                    .username(username)
                    .email(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail() : username + "@gradhire.com")
                    .fullName(request.getFullName() != null ? request.getFullName() : regNo)
                    .role(Role.STUDENT)
                    .password(passwordEncoder.encode("Admin@1234"))
                    .phone(request.getPhone())
                    .department(request.getDepartment())
                    .enabled(true)
                    .passwordChanged(false)
                    .forcePasswordChange(true)
                    .modifiedBy(uploadedBy)
                    .build();
            userRepository.save(user);

            Batch batch = null;
            if (request.getBatchYear() != null) {
                batch = batchRepository.findByYear(request.getBatchYear())
                        .orElseGet(() -> batchRepository.save(
                                Batch.builder().year(request.getBatchYear())
                                        .description("Batch of " + request.getBatchYear()).build()));
            }
            Student student = Student.builder()
                    .user(user)
                    .registerNumber(regNo)
                    .department(request.getDepartment())
                    .batch(batch)
                    .cgpa(request.getCgpa())
                    .gender(request.getGender())
                    .tenthPercent(request.getTenthPercent())
                    .twelfthPercent(request.getTwelfthPercent())
                    .ugPercentage(request.getUgPercentage())
                    .skills(request.getSkills())
                    .domain(request.getDomain())
                    .address(request.getAddress())
                    .phone(request.getPhone())
                    .email(request.getEmail())
                    .placementStatus(request.getPlacementStatus())
                    .status(request.getStatus())
                    .currentBacklogs(request.getCurrentBacklogs())
                    .historyOfBacklogs(request.getHistoryOfBacklogs())
                    .careerGap(request.getCareerGap())
                    .internships(request.getInternships())
                    .projects(request.getProjects())
                    .certifications(request.getCertifications())
                    .languages(request.getLanguages())
                    .dob(request.getDob())
                    .linkedinUrl(request.getLinkedinUrl())
                    .githubUrl(request.getGithubUrl())
                    .batchValue(request.getBatch())
                    .uploadHistoryId(uploadHistoryId)
                    .modifiedBy(uploadedBy)
                    .build();
            studentRepository.save(student);
            return "CREATED";
        }
    }

    /** Strips %, whitespace, and trailing .0 from percent strings so DB stores clean numeric values. */
    private String normalizePercent(String val) {
        if (val == null) return null;
        String cleaned = val.trim().replace("%", "").trim();
        if (cleaned.isBlank()) return null;
        // Remove trailing .0 from Excel numeric cells (e.g. "85.0" → "85")
        try {
            double d = Double.parseDouble(cleaned);
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        } catch (NumberFormatException e) {
            return cleaned; // Return as-is if not a number
        }
    }

    public void toggleUserStatus(Long userId, String modifiedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        notificationService.createNotificationByUsername(modifiedBy, "User Status Changed", "Changed status of user " + user.getUsername() + " to " + (user.isEnabled() ? "Enabled" : "Disabled"), "USER_UPDATE");
    }

    public List<User> getUsersByRole(Role role) { return userRepository.findByRole(role); }
    public List<User> getAllUsers() { return userRepository.findAll(); }
    public List<User> searchUsers(String q) { return userRepository.searchUsers(q); }
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional
    public void registerUser(RegisterRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username/Register number already exists: " + request.getUsername());
        }

        String roleName = request.getRole();
        Role role;
        if ("STAFF".equalsIgnoreCase(roleName)) {
            role = Role.STAFF;
        } else {
            role = Role.STUDENT;
        }

        if (!request.getPassword().matches("^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$")) {
            throw new BadRequestException("Password must be strong (min 8 chars, 1 uppercase, 1 number, 1 special char)");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail() : request.getUsername() + "@gradhire.com")
                .fullName(request.getFullName())
                .role(role)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .department(request.getDepartment())
                .enabled(true)
                .passwordChanged(true)
                .forcePasswordChange(false)
                .modifiedBy("self-registered")
                .build();
        userRepository.save(user);

        if (role == Role.STUDENT) {
            // Check if student record already exists (from Excel upload)
            var existing = studentRepository.findByRegisterNumber(request.getUsername());
            if (existing.isEmpty()) {
                Student student = Student.builder()
                        .user(user)
                        .registerNumber(request.getUsername())
                        .department(request.getDepartment())
                        .phone(request.getPhone())
                        .email(request.getEmail())
                        .modifiedBy("self-registered")
                        .build();
                studentRepository.save(student);
            } else {
                // Link existing student to new user
                Student s = existing.get();
                s.setUser(user);
                studentRepository.save(s);
            }
        } else if (role == Role.STAFF) {
            staffRepository.save(Staff.builder()
                    .user(user)
                    .department(request.getDepartment())
                    .build());
        }
    }

    // Keep backward compat
    @Transactional
    public void registerStudent(RegisterRequest request) {
        if (request.getRole() == null) request.setRole("STUDENT");
        registerUser(request);
    }

    @Transactional
    public User updateUser(Long userId, String fullName, String email, String phone, String department, String modifiedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (fullName != null && !fullName.isBlank()) user.setFullName(fullName);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
        if (department != null) user.setDepartment(department);
        User saved = userRepository.save(user);
        notificationService.createNotificationByUsername(modifiedBy, "User Updated", "Updated details for user " + user.getUsername(), "USER_UPDATE");
        return saved;
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Default reset password is Admin@1234 which satisfies the strong password rule
        String pass = newPassword != null ? newPassword : "Admin@1234";
        
        // Validate only custom passwords, skip for default
        if (newPassword != null && !pass.matches("^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#]).{8,}$")) {
            throw new BadRequestException("Password must be strong (min 8 chars, 1 uppercase, 1 number, 1 special char)");
        }

        user.setPassword(passwordEncoder.encode(pass));
        user.setPasswordChanged(false);
        user.setForcePasswordChange(true);
        userRepository.save(user);
        userRepository.flush();
    }

    @Transactional
    public void adminChangePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!newPassword.matches("^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$")) {
            throw new BadRequestException("Password must be strong (min 8 chars, 1 uppercase, 1 number, 1 special char)");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChanged(true);
        user.setForcePasswordChange(false);
        userRepository.save(user);
    }
}
