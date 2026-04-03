package com.thesis.securitystudy.service;

import com.thesis.securitystudy.exception.ResourceNotFoundException;
import com.thesis.securitystudy.model.Role;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return findUser(id);
    }

    public User updateUserRoles(Long id, Set<Role> roles) {
        User user = findUser(id);

        if (roles == null || roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roles must not be empty");
        }

        user.setRoles(roles);
        return userRepository.save(user);
    }

    public void deleteOrDisableUser(Long id, boolean permanent) {
        User user = findUser(id);

        if (permanent) {
            userRepository.delete(user);
        } else {
            user.setEnabled(false);
            userRepository.save(user);
        }
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}