package com.thesis.securitystudy.config;

import com.thesis.securitystudy.model.Role;
import com.thesis.securitystudy.model.User;
import com.thesis.securitystudy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User("admin", "admin@test.com",
                    passwordEncoder.encode("admin123"));
            admin.setRoles(Set.of(Role.ADMIN, Role.USER));
            userRepository.save(admin);

            User user = new User("testuser", "user@test.com",
                    passwordEncoder.encode("user1234"));
            user.setRoles(Set.of(Role.USER));
            userRepository.save(user);

            log.info("Seeded 2 test users: admin/admin123, testuser/user1234");
        }
    }
}
