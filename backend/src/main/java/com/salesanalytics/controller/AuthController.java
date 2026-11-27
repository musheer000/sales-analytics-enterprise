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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

    private static class BuiltInLock {
        int attempts = 0;
        LocalDateTime lockedUntil = null;
        boolean isLocked() { return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil); }
        void recordFailure() {
            attempts++;
            if (attempts >= 5) {
                long mins = (long) Math.min(1440, 10 * Math.pow(2, attempts - 5));
                lockedUntil = LocalDateTime.now().plusMinutes(mins);
            }
        }
        void reset() { attempts = 0; lockedUntil = null; }
        long remainingSeconds() { return isLocked() ? Math.max(1, Duration.between(LocalDateTime.now(), lockedUntil).getSeconds()) : 0; }
    }
    private final Map<String, BuiltInLock> builtInLocks = new ConcurrentHashMap<>();

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

    // ── REGISTER ─────────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@RequestBody Map<String, String> req) {
        String fullName = req.getOrDefault("fullName", "").trim();
        String email = req.getOrDefault("email", "").trim().toLowerCase();
        String password = req.getOrDefault("password", "");
        String requestedRole = req.getOrDefault("role", "USER").trim().toUpperCase();
        String role = "ADMIN".equals(requestedRole) ? "ADMIN" : "USER";

        if (fullName.isBlank() || email.isBlank() || !email.contains("@") || !email.contains(".")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Valid full name and email are required."));
        }
        if (password.length() < 8) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Password must be at least 8 characters."));
        }
        if ("admin".equalsIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("This username is reserved. Please use a valid personal email address."));
        }
        if ("USER".equals(role) && email.equalsIgnoreCase(ownerEmail)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Owner/Admin and Customer emails cannot be the same. Please use a different email."));
        }

        Optional<AppUser> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent() && existing.get().isVerified()) {
            String existingRole = existing.get().getRole() != null ? existing.get().getRole().toUpperCase() : "USER";
            if (!existingRole.equals(role)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("This email is already registered as " +
                                ("ADMIN".equals(existingRole) ? "an Admin" : "a Customer") +
                                ". Admin and Customer emails cannot be the same."));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("An account with this email already exists. Please sign in."));
        }

        AppUser user = existing.orElseGet(AppUser::new);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setVerified(false);
        user.resetFailedAttempts();
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
        user.resetFailedAttempts();
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok("Account verified & signed in",
                new LoginResponse(token, user.getEmail(), user.getRole())));
    }

    // ── LOGIN (with progressive lockout) ─────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        String username = request.getUsername().trim();

        String key = username.toLowerCase();
        Optional<AppUser> dbUserOpt = userRepository.findByEmailIgnoreCase(username);
        if (dbUserOpt.isPresent()) {
            AppUser dbUser = dbUserOpt.get();
            if (dbUser.isLocked()) {
                long remaining = dbUser.getRemainingLockSeconds();
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ApiResponse.error(formatLockMessage(remaining)));
            }
        } else {
            BuiltInLock lock = builtInLocks.get(key);
            if (lock != null && lock.isLocked()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ApiResponse.error(formatLockMessage(lock.remainingSeconds())));
            }
        }

        String userRole;
        try {
            Authentication authResult = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );
            String portal = request.getPortal() != null ? request.getPortal().trim().toUpperCase() : "";
            userRole = authResult.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("ROLE_USER")
                    .replace("ROLE_", "");
            if ("ADMIN".equals(portal) && !"ADMIN".equals(userRole)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("This is a Customer account. Please sign in on the Customer Store Portal."));
            }
            if ("USER".equals(portal) && "ADMIN".equals(userRole)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("This is an Admin/Owner account. Please sign in on the Admin Dashboard."));
            }
            dbUserOpt.ifPresent(u -> {
                u.resetFailedAttempts();
                u.setLastLoginAt(LocalDateTime.now());
                userRepository.save(u);
            });
            builtInLocks.remove(key);
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Account is not verified yet. Please complete OTP registration."));
        } catch (BadCredentialsException e) {
            if (dbUserOpt.isPresent()) {
                AppUser u = dbUserOpt.get();
                u.recordFailedAttempt();
                userRepository.save(u);
                if (u.isLocked()) {
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .body(ApiResponse.error("Too many failed attempts. " + formatLockMessage(u.getRemainingLockSeconds())));
                }
                int left = Math.max(0, 5 - u.getFailedLoginAttempts());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid credentials. " + left + " attempt(s) remaining before lockout."));
            } else {
                BuiltInLock lock = builtInLocks.computeIfAbsent(key, k -> new BuiltInLock());
                lock.recordFailure();
                if (lock.isLocked()) {
                    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                            .body(ApiResponse.error("Too many failed attempts. " + formatLockMessage(lock.remainingSeconds())));
                }
                int left = Math.max(0, 5 - lock.attempts);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid credentials. " + left + " attempt(s) remaining before lockout."));
            }
        }

        // Password verified — issue JWT immediately (no OTP required on login)
        String canonicalUsername = dbUserOpt.map(AppUser::getEmail).orElse(username);
        String token = jwtService.generateToken(canonicalUsername, userRole);

        return ResponseEntity.ok(ApiResponse.ok("Signed in successfully", Map.of(
                "otpRequired", false,
                "token", token,
                "username", canonicalUsername,
                "role", userRole
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
                .orElse("ROLE_USER")
                .replace("ROLE_", "");

        userRepository.findByEmailIgnoreCase(details.getUsername()).ifPresent(u -> {
            u.setLastLoginAt(LocalDateTime.now());
            u.resetFailedAttempts();
            userRepository.save(u);
        });

        String token = jwtService.generateToken(details.getUsername(), role);
        return ResponseEntity.ok(ApiResponse.ok("2FA Login successful",
                new LoginResponse(token, details.getUsername(), role)));
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

    // ── UPDATE PROFILE ────────────────────────────────────────────────────────
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @RequestBody Map<String, String> req, Authentication auth) {
        String email = auth.getName();
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String fullName = req.get("fullName");
        String phone = req.get("phone");
        String defaultAddress = req.get("defaultAddress");

        if (fullName != null && !fullName.isBlank()) user.setFullName(fullName.trim());
        if (phone != null) user.setPhone(phone.trim());
        if (defaultAddress != null) user.setDefaultAddress(defaultAddress.trim());
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.ok("Profile updated", Map.of(
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "defaultAddress", user.getDefaultAddress() != null ? user.getDefaultAddress() : ""
        )));
    }

    // ── CHANGE PASSWORD ───────────────────────────────────────────────────────
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody Map<String, String> req, Authentication auth) {
        String email = auth.getName();
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String currentPassword = req.getOrDefault("currentPassword", "");
        String newPassword = req.getOrDefault("newPassword", "");

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Current password is incorrect."));
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(ApiResponse.error("New password must be at least 8 characters."));
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }

    // ── GET CURRENT USER ──────────────────────────────────────────────────────
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
                .orElse("ROLE_USER")
                .replace("ROLE_", "");

        AppUser user = userRepository.findByEmailIgnoreCase(username).orElse(null);

        return ResponseEntity.ok(ApiResponse.ok("User authenticated", Map.of(
                "authenticated", true,
                "username", username,
                "role", role,
                "fullName", user != null ? user.getFullName() : username,
                "phone", user != null && user.getPhone() != null ? user.getPhone() : "",
                "defaultAddress", user != null && user.getDefaultAddress() != null ? user.getDefaultAddress() : ""
        )));
    }

    private String formatLockMessage(long remainingSeconds) {
        long minutes = remainingSeconds / 60;
        long seconds = remainingSeconds % 60;
        return minutes > 0
                ? String.format("Account locked. Try again in %d min %02d sec.", minutes, seconds)
                : String.format("Account locked. Try again in %d seconds.", seconds);
    }
}
