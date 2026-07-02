package com.isletmefinans.backend.service;

import com.isletmefinans.backend.dto.LoginRequest;
import com.isletmefinans.backend.dto.LoginResponse;
import com.isletmefinans.backend.dto.UserCreateRequest;
import com.isletmefinans.backend.dto.UserDeleteRequest;
import com.isletmefinans.backend.dto.UserResponse;
import com.isletmefinans.backend.entity.AppUser;
import com.isletmefinans.backend.entity.UserRole;
import com.isletmefinans.backend.exception.BusinessValidationException;
import com.isletmefinans.backend.exception.ResourceNotFoundException;
import com.isletmefinans.backend.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String mainAdminUsername;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.main-admin.username}") String mainAdminUsername
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.mainAdminUsername = normalizeUsername(mainAdminUsername);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsernameIgnoreCaseAndActiveTrue(normalizeUsername(request.username()))
                .orElseThrow(() -> new BusinessValidationException("Kullanici adi veya sifre hatali"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessValidationException("Kullanici adi veya sifre hatali");
        }

        return new LoginResponse(UserResponse.from(user), "Giris basarili");
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listActiveUsers() {
        return appUserRepository.findByActiveTrueOrderByUsernameAsc().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        verifyMainAdminPassword(request.mainAdminPassword());
        String normalizedUsername = normalizeUsername(request.username());

        if (appUserRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new BusinessValidationException("Bu kullanici adi zaten kullaniliyor");
        }

        AppUser user = new AppUser();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setActive(true);

        return UserResponse.from(appUserRepository.save(user));
    }

    @Transactional
    public void deactivateUser(Long userId, UserDeleteRequest request) {
        verifyMainAdminPassword(request.mainAdminPassword());

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanici bulunamadi: " + userId));

        if (user.getRole() == UserRole.MAIN_ADMIN || normalizeUsername(user.getUsername()).equals(mainAdminUsername)) {
            throw new BusinessValidationException("Ana admin kullanicisi silinemez");
        }

        user.setActive(false);
        appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public void verifyMainAdminPassword(String rawPassword) {
        AppUser mainAdmin = appUserRepository.findByUsernameIgnoreCaseAndActiveTrue(mainAdminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Ana admin kullanicisi bulunamadi"));

        if (!passwordEncoder.matches(rawPassword, mainAdmin.getPasswordHash())) {
            throw new BusinessValidationException("Ana admin sifresi hatali");
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
