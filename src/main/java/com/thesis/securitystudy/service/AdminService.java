package com.thesis.securitystudy.service;

import com.thesis.securitystudy.dto.RoleUpdateRequest;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;

    @Autowired
    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public User updateRoles(Long id, RoleUpdateRequest request) {
        User user = getUser(id);
        user.setRoles(request.getRoles());
        return userRepository.save(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User nicht gefunden"));

        // Versuche, Account zu deaktivieren; falls Feld/Setter nicht vorhanden ist, lösche den Eintrag.
        try {
            user.setEnabled(false);
            userRepository.save(user);
        } catch (NoSuchMethodError | RuntimeException ignored) {
            userRepository.deleteById(id);
        }
    }
}
