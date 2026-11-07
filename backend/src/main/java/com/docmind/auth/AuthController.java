package com.docmind.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docmind.auth.dto.AuthDtos.AuthResponse;
import com.docmind.auth.dto.AuthDtos.LoginRequest;
import com.docmind.auth.dto.AuthDtos.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        User user = User.builder()
            .email(req.email())
            .passwordHash(passwordEncoder.encode(req.password()))
            .name(req.name())
            .build();
        userRepository.save(user);
        return ResponseEntity.ok(toResponse(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return userRepository.findByEmail(req.email())
            .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getEmail(), user.getName());
    }
}
