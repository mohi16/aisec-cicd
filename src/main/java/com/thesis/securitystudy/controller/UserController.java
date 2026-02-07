package com.thesis.securitystudy.controller;

import com.thesis.securitystudy.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(Authentication auth) {
        Map<String, Object> userInfo = Map.of(
                "username", auth.getName(),
                "authorities", auth.getAuthorities()
        );
        return ResponseEntity.ok(ApiResponse.ok("Current user", userInfo));
    }
}
