package com.docmind.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 60) String name) {
    }

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
    }

    public record AuthResponse(String token, Long userId, String email, String name) {
    }
}
