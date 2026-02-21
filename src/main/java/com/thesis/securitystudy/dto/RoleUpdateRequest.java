package com.thesis.securitystudy.dto;

import com.thesis.securitystudy.model.Role;

import java.util.Set;

public class RoleUpdateRequest {
    private Set<Role> roles;

    public RoleUpdateRequest() {}

    public RoleUpdateRequest(Set<Role> roles) {
        this.roles = roles;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
