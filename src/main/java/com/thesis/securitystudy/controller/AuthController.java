package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.*;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final HttpServletRequest request;

    public AuthController(AuthService authService, HttpServletRequest request) {
        this.authService = authService;
        this.request = request;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpServletRequest) {
        authService.register(request, httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok("User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        AuthResponse authResponse = authService.login(request, httpServletRequest);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", authResponse));
    }
}
