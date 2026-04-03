package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.Role;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public List<User> listUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User updateRoles(Long id, Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roles must not be empty");
        }

        User user = getUserById(id);
        user.setRoles(roles);
        return userRepository.save(user);
    }


    public void deleteOrDisableUser(Long id) {
        User user = getUserById(id);

        // simpler / safer default: disable instead of hard delete
        user.setEnabled(false);
        userRepository.save(user);
    }
}