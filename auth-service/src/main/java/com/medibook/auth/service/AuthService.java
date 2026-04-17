package com.medibook.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.medibook.auth.entity.User;
import com.medibook.auth.repository.UserRepository;

@Service
public class AuthService {

    @Autowired
    private UserRepository repo;

    public User register(User user) {
        return repo.save(user);
    }

    public User findByEmail(String email) {
        return repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
