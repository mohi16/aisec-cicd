package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.AuthResponse;
import com.thesis.securitystudy.dto.LoginRequest;
import com.thesis.securitystudy.dto.RegisterRequest;
import com.thesis.securitystudy.model.Role;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.auditService = auditService;
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );
        User saved = userRepository.save(user);

        // Record audit event for registration
        String ip = getClientIp();
        auditService.record("REGISTER", "User", saved.getId(), saved.getUsername(),
                "New user registered", ip, com.thesis.securitystudy.model.AuditLog.AuditLevel.INFO);

        return saved;
    }

    public AuthResponse login(LoginRequest request) {
        String ip = getClientIp();
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));

            String token = jwtUtil.generateToken(request.getUsername());

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow();

            // Successful login audit
            auditService.record("LOGIN_SUCCESS", "User", user.getId(), user.getUsername(),
                    "User logged in", ip, com.thesis.securitystudy.model.AuditLog.AuditLevel.INFO);

            return new AuthResponse(
                    token,
                    user.getUsername(),
                    user.getRoles().stream().map(Role::name).collect(Collectors.toSet())
            );
        } catch (AuthenticationException ex) {
            // Record failed login attempt (username may or may not exist)
            auditService.record("LOGIN_FAILURE", "User", null, request.getUsername(),
                    "Failed login attempt: " + ex.getMessage(), ip, com.thesis.securitystudy.model.AuditLog.AuditLevel.WARNING);
            // rethrow to let existing exception handling handle response
            throw ex;
        }
    }

    /**
     * Example password-related action: change password for a user.
     * If your project already has a password-change method, integrate audit calls there instead.
     */
    public void changePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        String ip = getClientIp();
        auditService.record("PASSWORD_CHANGE", "User", user.getId(), user.getUsername(),
                "Password changed (by user)", ip, com.thesis.securitystudy.model.AuditLog.AuditLevel.WARNING);
    }

    /**
     * Helper: Try to resolve client's IP address from the current HTTP request context.
     * Falls back to null if not in request scope.
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr == null) {
                return null;
            }
            HttpServletRequest request = attr.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception ex) {
            // don't allow IP resolution problems to break auth flows
            return null;
        }
    }
}
