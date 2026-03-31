package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.AuthResponse;
import com.thesis.securitystudy.dto.LoginRequest;
import com.thesis.securitystudy.dto.RegisterRequest;
import com.thesis.securitystudy.model.AuditLog;
import com.thesis.securitystudy.model.Role;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
                       AuthenticationManager authenticationManager, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.auditService = auditService;
    }

    public User register(RegisterRequest request, HttpServletRequest httpServletRequest) {
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
        user = userRepository.save(user);
        auditService.record(
                "REGISTER",
                "User",
                user.getId(),
                user.getUsername(),
                "New user registration",
                httpServletRequest.getRemoteAddr(),
                AuditLog.AuditLevel.INFO
        );
        return user;
    }

    public AuthResponse login(LoginRequest request, HttpServletRequest httpServletRequest) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));
        } catch (AuthenticationException ex) {
            userRepository.findByUsername(request.getUsername()).ifPresentOrElse(
                    user -> auditService.record(
                            "LOGIN_FAILED",
                            "User",
                            user.getId(),
                            user.getUsername(),
                            "Failed login attempt",
                            httpServletRequest.getRemoteAddr(),
                            AuditLog.AuditLevel.WARNING
                    ),
                    () -> auditService.record(
                            "LOGIN_FAILED",
                            "User",
                            null,
                            request.getUsername(),
                            "Failed login attempt (unknown user)",
                            httpServletRequest.getRemoteAddr(),
                            AuditLog.AuditLevel.WARNING
                    )
            );

            throw ex;
        }

        String token = jwtUtil.generateToken(request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow();

        auditService.record(
                "LOGIN",
                "User",
                user.getId(),
                user.getUsername(),
                "User logged in",
                null,
                AuditLog.AuditLevel.INFO
        );

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getRoles().stream().map(Role::name).collect(Collectors.toSet())
        );
    }
}