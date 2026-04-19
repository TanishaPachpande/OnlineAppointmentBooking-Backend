package com.medibook.auth.service;

import com.medibook.auth.dto.AuthResponseDto;
import com.medibook.auth.dto.LoginRequestDto;
import com.medibook.auth.dto.RegisterRequestDto;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto requestDto);

    AuthResponseDto login(LoginRequestDto requestDto);

    AuthResponseDto getUserByEmail(String email);

    AuthResponseDto getUserById(Long userId);

    String deactivateAccount(Long userId);
}