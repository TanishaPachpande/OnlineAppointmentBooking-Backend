package com.medibook.auth.service;

import com.medibook.auth.dto.*;

public interface AuthService {

    AuthResponseDto register(RegisterRequestDto requestDto);
    AuthResponseDto login(LoginRequestDto requestDto);
    AuthResponseDto getUserByEmail(String email);
    AuthResponseDto getUserById(Long userId);
    String deactivateAccount(Long userId);

    AuthResponseDto registerWithOtp(RegisterWithOtpRequestDto requestDto);
}
