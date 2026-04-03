package com.thesis.securitystudy.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.HashSet;
import java.util.Set;

public class RoleUpdateRequest {

    @NotEmpty
    private Set<String> roles = new HashSet<>();

    public RoleUpdateRequest() {
    }

    public RoleUpdateRequest(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}