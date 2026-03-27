package com.squadup.security;

import com.squadup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación de UserDetailsService para Spring Security.
 * Carga el usuario por email para la autenticación.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPasswordHash() != null ? user.getPasswordHash() : "",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + email));
    }
}
