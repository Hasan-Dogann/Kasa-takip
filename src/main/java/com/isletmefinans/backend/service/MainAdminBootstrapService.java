package com.isletmefinans.backend.service;

import com.isletmefinans.backend.entity.AppUser;
import com.isletmefinans.backend.entity.UserRole;
import com.isletmefinans.backend.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class MainAdminBootstrapService implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public MainAdminBootstrapService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.main-admin.username}") String username,
            @Value("${app.security.main-admin.password}") String password
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username == null ? "admin" : username.trim().toLowerCase();
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.existsByRoleAndActiveTrue(UserRole.MAIN_ADMIN)) {
            return;
        }

        AppUser mainAdmin = new AppUser();
        mainAdmin.setUsername(username);
        mainAdmin.setPasswordHash(passwordEncoder.encode(password));
        mainAdmin.setRole(UserRole.MAIN_ADMIN);
        mainAdmin.setActive(true);
        appUserRepository.save(mainAdmin);
    }
}
