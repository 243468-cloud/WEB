package com.squadup.dto;

import lombok.Builder;
import lombok.Data;

/** DTO de respuesta tras autenticación exitosa */
@Data
@Builder
public class AuthResponse {
    private String token;
    private String tokenType; // "Bearer"
    private Long userId;
    private String username;
    private String email;
    private String avatarUrl;
}
