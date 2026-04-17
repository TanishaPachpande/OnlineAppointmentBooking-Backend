package com.medibook.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.medibook.auth.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}