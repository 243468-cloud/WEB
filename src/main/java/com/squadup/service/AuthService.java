package com.squadup.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.squadup.dto.AuthResponse;
import com.squadup.dto.LoginRequest;
import com.squadup.dto.RegisterRequest;
import com.squadup.entity.User;
import com.squadup.exception.BadRequestException;
import com.squadup.exception.ResourceNotFoundException;
import com.squadup.repository.UserRepository;
import com.squadup.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Servicio de autenticación: registro local, login y soporte OAuth2.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${google.client-id}")
    private String googleClientId;

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

    /**
     * Verifica el ID Token emitido por Google Sign-In en el frontend.
     * Si el usuario no existe en BD lo crea. Siempre devuelve el JWT propio de SquadUp.
     *
     * @param idToken token JWT emitido por Google (viene del frontend)
     */
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        GoogleIdToken.Payload payload = verifyGoogleToken(idToken);

        String email    = payload.getEmail().toLowerCase();
        String name     = (String) payload.get("name");
        String picture  = (String) payload.get("picture");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // Generar username único a partir de la parte local del email
                    String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
                    String username = resolveUniqueUsername(baseUsername);

                    return userRepository.save(User.builder()
                            .email(email)
                            .fullName(name != null ? name : baseUsername)
                            .username(username)
                            .avatarUrl(picture)
                            .passwordHash(null) // Sin contraseña — solo OAuth
                            .build());
                });

        // Actualizar avatar si cambió en Google
        if (picture != null && !picture.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(picture);
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(user, token);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Valida el idToken contra los servidores de Google.
     * Lanza BadRequestException si el token es inválido o expiró.
     */
    private GoogleIdToken.Payload verifyGoogleToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new BadRequestException("Token de Google inválido o expirado");
            }
            return token.getPayload();
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Error al verificar token de Google: " + e.getMessage());
        }
    }

    /**
     * Si el username base ya existe, agrega un sufijo numérico hasta encontrar uno libre.
     * Ejemplo: "juan" → "juan2" → "juan3"
     */
    private String resolveUniqueUsername(String base) {
        if (!userRepository.existsByUsername(base)) return base;
        int suffix = 2;
        while (userRepository.existsByUsername(base + suffix)) suffix++;
        return base + suffix;
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
