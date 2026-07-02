package com.isletmefinans.backend.repository;

import com.isletmefinans.backend.entity.AppUser;
import com.isletmefinans.backend.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameIgnoreCaseAndActiveTrue(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByRoleAndActiveTrue(UserRole role);

    List<AppUser> findByActiveTrueOrderByUsernameAsc();
}
