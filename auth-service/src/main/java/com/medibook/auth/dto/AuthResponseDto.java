package com.medibook.auth.dto;

import com.medibook.auth.entity.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {
    private Long userId;
    private String fullName;
    private String email;
    private String token;
    private Role role;
    private String message;
}