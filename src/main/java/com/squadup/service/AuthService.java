package com.squadup.service;

import com.squadup.dto.AuthResponse;
import com.squadup.dto.LoginRequest;
import com.squadup.dto.RegisterRequest;
import com.squadup.entity.User;
import com.squadup.exception.BadRequestException;
import com.squadup.exception.ResourceNotFoundException;
import com.squadup.repository.UserRepository;
import com.squadup.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación: registro local, login y soporte OAuth2.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** Vista "Crear Cuenta" */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BadRequestException("Las contraseñas no coinciden");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("El correo ya está registrado");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BadRequestException("El apodo ya está en uso");
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .username(req.getUsername())
                .email(req.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .build();

        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail());

        return buildAuthResponse(user, token);
    }

    /** Vista "Iniciar Sesión" */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Credenciales inválidas");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
