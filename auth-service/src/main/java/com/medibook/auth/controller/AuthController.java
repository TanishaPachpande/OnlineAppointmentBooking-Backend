package com.medibook.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.medibook.auth.entity.User;
import com.medibook.auth.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService service;
    
    @GetMapping("/test")
    public String test() {
        return "Auth working";
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@Valid @RequestBody User user) {
        return ResponseEntity.ok(service.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        User dbUser = service.findByEmail(user.getEmail());

        if (!dbUser.getPassword().equals(user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return ResponseEntity.ok("Login successful");
    }
}