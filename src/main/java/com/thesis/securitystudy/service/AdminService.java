package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.Role;
import java.util.List;
import java.util.Set;

public interface AdminService {

    record UserAdminDto(Long id, String username, String email, Set<Role> roles, boolean enabled) {}

    List<UserAdminDto> getAllUsers();

    UserAdminDto getUserById(Long id);

    UserAdminDto updateUserRoles(Long id, Set<Role> roles);

    /**
     * Disable or permanently delete a user.
     * @param id user id
     * @param permanent if true -> permanently delete from DB; otherwise set enabled = false
     */
    void deleteOrDisableUser(Long id, boolean permanent);
}
