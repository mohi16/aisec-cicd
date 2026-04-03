package com.thesis.securitystudy.dto;

import java.util.HashSet;
import java.util.Set;

public class RoleUpdateRequest {

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
