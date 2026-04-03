package com.thesis.securitystudy.dto;

import com.thesis.securitystudy.model.Role;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public class RoleUpdateRequest {

    @NotEmpty
    private Set<Role> roles;

    public RoleUpdateRequest() {
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}