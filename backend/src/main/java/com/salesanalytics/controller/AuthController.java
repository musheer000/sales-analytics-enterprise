package com.salesanalytics.controller;

import com.salesanalytics.dto.ApiResponse;
import com.salesanalytics.dto.LoginRequest;
import com.salesanalytics.dto.LoginResponse;
import com.salesanalytics.entity.AppUser;
import com.salesanalytics.repository.AppUserRepository;
import com.salesanalytics.security.JwtService;
import com.salesanalytics.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;

    @Value("${app.mail.username:shaikmusheer07@gmail.com}")
    private String ownerEmail;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            OtpService otpService,
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody Map<String, String> req) {
        String fullName = req.getOrDefault("fullName", "").trim();
        String email = req.getOrDefault("email", "").trim().toLowerCase();
        String password = req.getOrDefault("password", "");
        String requestedRole = req.getOrDefault("role", "ADMIN").trim().toUpperCase();
        String role = ("ADMIN".equals(requestedRole) || "VIEWER".equals(requestedRole)) ? requestedRole : "VIEWER";

        if (fullName.isBlank() || email.isBlank() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Valid full name and email are required."));
        }
        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Password must be at least 6 characters."));
        }

        Optional<AppUser> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent() && existing.get().isVerified()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("An account with this email already exists. Please sign in."));
        }

        AppUser user = existing.orElseGet(AppUser::new);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setVerified(false);
        userRepository.save(user);

        otpService.sendOtp(email, "REGISTER", fullName);
        String masked = otpService.maskEmail(email);
        return ResponseEntity.ok(ApiResponse.ok("Verification OTP sent to " + masked, Map.of(
                "otpRequired", true,
                "purpose", "REGISTER",
                "email", email,
                "maskedEmail", masked,
                "expiresInSeconds", 300
        )));
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyRegistrationOtp(@RequestBody Map<String, String> req) {
        String email = req.getOrDefault("email", "").trim().toLowerCase();
        String otp = req.getOrDefault("otp", "").trim();

        otpService.verifyOtp(email, "REGISTER", otp);

        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));
        user.setVerified(true);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok("Account verified & signed in", new LoginResponse(token, user.getEmail(), user.getRole())));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        String username = request.getUsername().trim();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Account is not verified yet. Please complete OTP registration."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username/email or password."));
        }

        String targetEmail = username.contains("@") ? username.toLowerCase() : ownerEmail;
        otpService.sendOtp(targetEmail, "LOGIN", username);
        String masked = otpService.maskEmail(targetEmail);

        return ResponseEntity.ok(ApiResponse.ok("2FA OTP sent to " + masked, Map.of(
                "otpRequired", true,
                "purpose", "LOGIN",
                "username", username,
                "email", targetEmail,
                "maskedEmail", masked,
                "expiresInSeconds", 300
        )));
    }

    @PostMapping("/login/verify-otp")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyLoginOtp(@RequestBody Map<String, String> req) {
        String username = req.getOrDefault("username", "").trim();
        String email = req.getOrDefault("email", "").trim().toLowerCase();
        String otp = req.getOrDefault("otp", "").trim();

        if (email.isBlank()) {
            email = username.contains("@") ? username.toLowerCase() : ownerEmail;
        }
        otpService.verifyOtp(email, "LOGIN", otp);

        UserDetails details = userDetailsService.loadUserByUsername(username);
        String role = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_VIEWER")
                .replace("ROLE_", "");

        userRepository.findByEmailIgnoreCase(details.getUsername()).ifPresent(u -> {
            u.setLastLoginAt(LocalDateTime.now());
            userRepository.save(u);
        });

        String token = jwtService.generateToken(details.getUsername(), role);
        return ResponseEntity.ok(ApiResponse.ok("2FA Login successful", new LoginResponse(token, details.getUsername(), role)));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendOtp(@RequestBody Map<String, String> req) {
        String email = req.getOrDefault("email", "").trim().toLowerCase();
        String purpose = req.getOrDefault("purpose", "LOGIN").trim().toUpperCase();
        String name = req.getOrDefault("name", email);

        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email is required to resend OTP."));
        }
        otpService.sendOtp(email, purpose, name);
        String masked = otpService.maskEmail(email);
        return ResponseEntity.ok(ApiResponse.ok("New OTP sent to " + masked, Map.of(
                "maskedEmail", masked,
                "expiresInSeconds", 300
        )));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.ok(ApiResponse.ok("Unauthenticated", Map.of("authenticated", false)));
        }

        String username = auth.getName();
        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_VIEWER")
                .replace("ROLE_", "");

        return ResponseEntity.ok(ApiResponse.ok("User authenticated", Map.of(
                "authenticated", true,
                "username", username,
                "role", role
        )));
    }
}
