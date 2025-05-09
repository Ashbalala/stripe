package edu.cit.taskbounty.controller;

import edu.cit.taskbounty.dto.UserUpdateDTO;
import edu.cit.taskbounty.model.User;
import edu.cit.taskbounty.repository.UserRepository;
import edu.cit.taskbounty.service.AuthService;
import edu.cit.taskbounty.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody User user) {
        try {
            User registeredUser = authService.register(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "success",
                    "data", Map.of(
                            "userId", registeredUser.getId(),
                            "username", registeredUser.getUsername(),
                            "email", registeredUser.getEmail(),
                            "birthDate", registeredUser.getBirthDate(),
                            "countryCode", registeredUser.getCountryCode(),
                            "message", "User registered successfully. Please verify your email."
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> verifyUser(@RequestParam("code") Long code) {
        String username = getAuthenticatedUsername();
        return authService.verifyUser(username, code);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String identifier = credentials.get("identifier");
        String password = credentials.get("password");

        try {
            Optional<User> userOpt = authService.login(identifier, password);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String token = jwtUtil.generateJwtToken(user.getUsername());

                ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .maxAge(24 * 60 * 60)
                        .build();

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                        .body(Map.of(
                                "status", "success",
                                "data", Map.of(
                                        "userId", user.getId(),
                                        "username", user.getUsername(),
                                        "message", "Login successful"
                                )
                        ));
            }
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Invalid username/email or password"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/resend_code")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> resendVerificationCode() {
        try {
            String username = getAuthenticatedUsername();
            authService.resendVerificationCode(username);
            return ResponseEntity.ok("Verification code resent successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping("/change_email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changeEmail(@RequestParam String newEmail) {
        try {
            String username = getAuthenticatedUsername();
            authService.changeEmail(username, newEmail);
            return ResponseEntity.ok("Email change requested. Please verify your new email.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PatchMapping("/update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserUpdateDTO updateDTO) {
        try {
            // Validate that at least one field is provided
            if (!updateDTO.hasAtLeastOneField()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", "At least one field must be provided"));
            }

            String username = getAuthenticatedUsername();
            User user = userRepository.findByUsername(username);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "User not found"));
            }

            // Update fields if provided
            if (updateDTO.getPassword() != null) {
                user.setPassword(passwordEncoder.encode(updateDTO.getPassword()));
            }

            if (updateDTO.getUsername() != null) {
                if (userRepository.findByUsername(updateDTO.getUsername()) != null &&
                        !updateDTO.getUsername().equals(username)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("status", "error", "message", "Username already exists"));
                }
                user.setUsername(updateDTO.getUsername());
            }

            if (updateDTO.getCountryCode() != null) {
                user.setCountryCode(updateDTO.getCountryCode());
            }

            if (updateDTO.getBirthDate() != null) {
                user.setBirthDate(updateDTO.getBirthDate());
            }

            userRepository.save(user);

            return ResponseEntity.ok()
                    .body(Map.of(
                            "status", "success",
                            "data", Map.of(
                                    "userId", user.getId(),
                                    "username", user.getUsername(),
                                    "email", user.getEmail(),
                                    "birthDate", user.getBirthDate(),
                                    "countryCode", user.getCountryCode(),
                                    "message", "Profile updated successfully"
                            )
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUserProfile() {
        try {
            String username = getAuthenticatedUsername();
            User user = userRepository.findByUsername(username);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "User not found"));
            }

            return ResponseEntity.ok()
                    .body(Map.of(
                            "status", "success",
                            "data", Map.of(
                                    "userId", user.getId(),
                                    "username", user.getUsername(),
                                    "email", user.getEmail(),
                                    "birthDate", user.getBirthDate(),
                                    "countryCode", user.getCountryCode(),
                                    "verified", user.isVerified(),
                                    "dateCreated", user.getDateCreated()
                            )
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/profile/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserProfileById(@PathVariable String userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "User not found"));
            }

            User user = userOpt.get();
            return ResponseEntity.ok()
                    .body(Map.of(
                            "status", "success",
                            "data", Map.of(
                                    "userId", user.getId(),
                                    "username", user.getUsername(),
                                    "email", user.getEmail(),
                                    "birthDate", user.getBirthDate(),
                                    "countryCode", user.getCountryCode(),
                                    "verified", user.isVerified(),
                                    "dateCreated", user.getDateCreated()
                            )
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private String getAuthenticatedUsername() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUsername();
    }
}