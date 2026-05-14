package com.medibook.auth.repository;

import com.medibook.auth.entity.Role;
import com.medibook.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserId(Long userId);

    Boolean existsByEmail(String email);

    Boolean existsByPhone(String phone);

    List<User> findAllByRole(Role role);
}