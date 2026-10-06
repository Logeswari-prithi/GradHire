package com.gradhire.service;

import com.gradhire.dto.*;
import com.gradhire.entity.User;
import com.gradhire.exception.BadRequestException;
import com.gradhire.exception.ResourceNotFoundException;
import com.gradhire.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gradhire.entity.Role;

import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User login(String username, String password, String role) {

        // STEP 1: Fetch user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("Invalid username"));

        // STEP 2: CHECK PASSWORD USING BCRYPT
        log.debug("Login attempt for user: {}", username);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // Fallback for plain-text passwords
            if (password.equals(user.getPassword())) {
                // Password is correct but stored as plain text. Re-encode and save.
                log.info("Migrating plain text password to BCrypt for user: {}", username);
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
            } else {
                throw new BadRequestException("Invalid password");
            }
        }

        // STEP 3: CHECK ROLE
        if (!user.getRole().name().equals(role)) {
            throw new BadRequestException("Invalid role selection");
        }

        // STEP 4: CHECK IF ENABLED
        if (!user.isEnabled()) {
            throw new BadRequestException("Account is disabled. Contact admin.");
        }

        log.info("Login successful for user: {} with role: {}", username, role);
        return user;
    }

    @Transactional
    public void changePassword(String username, PasswordChangeRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Staff can change username
        if (user.getRole() == Role.STAFF && request.getNewUsername() != null && !request.getNewUsername().isBlank()) {
            if (!user.getUsername().equals(request.getNewUsername()) && userRepository.existsByUsername(request.getNewUsername())) {
                throw new BadRequestException("Username already exists");
            }
            user.setUsername(request.getNewUsername().trim());
        }

        if (!request.getNewPassword().matches("^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#]).{8,}$")) {
            throw new BadRequestException("Password must be strong (min 8 chars, 1 uppercase, 1 number, 1 special char)");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChanged(true);
        user.setForcePasswordChange(false);
        userRepository.save(user);
        userRepository.flush();
        log.info("Password changed successfully for user: {}", username);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Default reset password if null
        String pass = java.util.Optional.ofNullable(newPassword).orElse("Admin@1234");

        if (newPassword != null && !pass.matches("^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#]).{8,}$")) {
            throw new BadRequestException("Password must be strong (min 8 chars, 1 uppercase, 1 number, 1 special char)");
        }

        user.setPassword(passwordEncoder.encode(pass));
        user.setPasswordChanged(false);
        user.setForcePasswordChange(true);
        userRepository.save(user);
        userRepository.flush();
        log.info("Password reset for user ID: {}", userId);
    }
}
