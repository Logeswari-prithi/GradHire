package com.gradhire.controller;

import com.gradhire.dto.*;
import com.gradhire.entity.User;
import com.gradhire.service.AuthService;
import com.gradhire.service.UserService;
import com.gradhire.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request.getUsername(), request.getPassword(), request.getRole());
        
        LoginResponse response = LoginResponse.builder()
                .token(jwtUtil.generateToken(user.getUsername()))
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .passwordChanged(user.isPasswordChanged())
                .userId(user.getId())
                .firstLogin(!user.isPasswordChanged() || user.isForcePasswordChange())
                .message("Login successful")
                .build();
                
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        userService.registerUser(request);
        String roleName = "STAFF".equalsIgnoreCase(request.getRole()) ? "Staff" : "Student";
        return ResponseEntity.ok(ApiResponse.success(roleName + " registered successfully. You can now log in.", null));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication auth,
            @Valid @RequestBody PasswordChangeRequest request) {
        authService.changePassword(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}
