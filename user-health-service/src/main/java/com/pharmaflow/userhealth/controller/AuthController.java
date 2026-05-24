package com.pharmaflow.userhealth.controller;

import com.pharmaflow.userhealth.enums.Role;
import com.pharmaflow.userhealth.models.PatientProfile;
import com.pharmaflow.userhealth.models.RefreshToken;
import com.pharmaflow.userhealth.models.User;
import com.pharmaflow.userhealth.repositories.UserRepository;
import com.pharmaflow.userhealth.security.JwtUtil;
import com.pharmaflow.userhealth.security.TokenBlacklistService;
import com.pharmaflow.userhealth.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Complete authentication with JWT, refresh tokens, roles, and logout")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;

    // ── DTOs ──────────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "First name is required")
        private String firstName;

        @NotBlank(message = "Last name is required")
        private String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private String role; // Optional: ROLE_USER, ROLE_DOCTOR, ROLE_PHARMACIST, ROLE_ADMIN
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private Long userId;
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private String email;
        private String firstName;
        private String lastName;
        private List<String> roles;
        private long expiresIn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "New password must be at least 6 characters")
        private String newPassword;
    }

    // ── Endpoints ─────────────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Login with email and password", description = "Returns JWT access token and refresh token")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid email or password"));
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Invalid email or password"));
        }

        // Get user roles (default to ROLE_USER if not set)
        List<String> roles = user.getRole() != null ?
            List.of(user.getRole()) : List.of(Role.ROLE_USER.name());

        String accessToken = jwtUtil.generateToken(user.getEmail(), roles, user.getId(),
                user.getFirstName(), user.getLastName());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return ResponseEntity.ok(new AuthResponse(
                user.getId(),
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles,
                jwtUtil.getExpirationDuration()
        ));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account", description = "Creates user with specified role (default: ROLE_USER)")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Conflict", "message", "User with this email already exists"));
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Set role (default to ROLE_USER)
        String role = request.getRole() != null && isValidRole(request.getRole()) ?
            request.getRole() : Role.ROLE_USER.name();
        user.setRole(role);

        // Always create an empty patient profile so health endpoints work immediately
        PatientProfile emptyProfile = new PatientProfile();
        emptyProfile.setAllergies(new java.util.ArrayList<>());
        emptyProfile.setTherapies(new java.util.ArrayList<>());
        user.setPatientProfile(emptyProfile);

        userRepository.save(user);

        List<String> roles = List.of(role);
        String accessToken = jwtUtil.generateToken(user.getEmail(), roles, user.getId(),
                user.getFirstName(), user.getLastName());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(
                user.getId(),
                accessToken,
                refreshToken.getToken(),
                "Bearer",
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles,
                jwtUtil.getExpirationDuration()
        ));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generate new access token using refresh token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserId)
                .map(userId -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));

                    List<String> roles = user.getRole() != null ?
                        List.of(user.getRole()) : List.of(Role.ROLE_USER.name());

                    String newAccessToken = jwtUtil.generateToken(user.getEmail(), roles, user.getId(),
                            user.getFirstName(), user.getLastName());

                    return ResponseEntity.ok(Map.of(
                            "accessToken", newAccessToken,
                            "refreshToken", requestRefreshToken,
                            "tokenType", "Bearer",
                            "userId", user.getId(),
                            "expiresIn", jwtUtil.getExpirationDuration()
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Invalid refresh token")));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates access token and refresh token")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader,
                                    @RequestBody(required = false) Map<String, String> body) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String accessToken = authHeader.substring(7);

                // Blacklist access token
                long expirationTime = jwtUtil.getExpirationTime(accessToken);
                tokenBlacklistService.blacklistToken(accessToken, expirationTime);

                // Revoke refresh token if provided
                if (body != null && body.containsKey("refreshToken")) {
                    refreshTokenService.revokeToken(body.get("refreshToken"));
                }

                return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
            }

            return ResponseEntity.badRequest().body(Map.of("error", "Invalid token"));
        } catch (Exception e) {
            e.printStackTrace(); // Log exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Logout failed", "message", e.getMessage()));
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token", description = "Used by gateway to validate tokens (checks blacklist)")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false));
        }

        try {
            String token = authHeader.substring(7);

            // Check if token is blacklisted
            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "message", "Token has been revoked"));
            }

            String email = jwtUtil.extractEmail(token);
            Long userId = jwtUtil.extractUserId(token);
            List<String> roles = jwtUtil.extractRoles(token);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("valid", true);
            if (userId != null) {
                response.put("userId", userId);
            }
            response.put("email", email);
            response.put("roles", roles);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "Token invalid or expired"));
        }
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Changes password for the authenticated user")
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "X-Username", required = false) String emailFromGateway,
            org.springframework.security.core.Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {

        String email = emailFromGateway != null ? emailFromGateway
                : (authentication != null ? authentication.getName() : null);

        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Cannot identify user"));
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not Found", "message", "User not found"));
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Bad Request", "message", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private boolean isValidRole(String role) {
        try {
            Role.valueOf(role);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
