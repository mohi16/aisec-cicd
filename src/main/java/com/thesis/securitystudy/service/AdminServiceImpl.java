package com.thesis.securitystudy.service;

import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.model.Role;
import com.thesis.securitystudy.repository.UserRepository;
import com.thesis.securitystudy.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;

    public AdminServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private AdminService.UserAdminDto toDto(User u) {
        return new AdminService.UserAdminDto(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getRoles(),
                u.isEnabled()
        );
    }

    @Override
    public List<AdminService.UserAdminDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public AdminService.UserAdminDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toDto(user);
    }

    @Override
    public AdminService.UserAdminDto updateUserRoles(Long id, Set<Role> roles) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setRoles(roles);
        user = userRepository.save(user);
        return toDto(user);
    }

    @Override
    public void deleteOrDisableUser(Long id, boolean permanent) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (permanent) {
            userRepository.delete(user);
        } else {
            user.setEnabled(false);
            userRepository.save(user);
        }
    }
}
