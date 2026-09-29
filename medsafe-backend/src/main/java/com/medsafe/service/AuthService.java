package com.medsafe.service;

import com.medsafe.dto.AuthResponse;
import com.medsafe.dto.LoginRequest;
import com.medsafe.dto.RegisterRequest;
import com.medsafe.entity.User;
import com.medsafe.exception.DuplicateResourceException;
import com.medsafe.exception.UnauthorizedException;
import com.medsafe.repository.UserRepository;
import com.medsafe.security.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new DuplicateResourceException("An account with email " + cleanEmail + " already exists.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getName().trim(), cleanEmail, encodedPassword);
        User savedUser = userRepository.save(user);

        String token = tokenService.generateToken(savedUser.getUserId(), savedUser.getEmail());
        return new AuthResponse(savedUser.getUserId(), savedUser.getName(), savedUser.getEmail(), token, "Registration successful");
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = tokenService.generateToken(user.getUserId(), user.getEmail());
        return new AuthResponse(user.getUserId(), user.getName(), user.getEmail(), token, "Login successful");
    }
}
