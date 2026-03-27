package com.squadup.dto;

import com.squadup.entity.enums.LobbyPrivacy;
import com.squadup.entity.enums.LobbyType;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/** DTO de respuesta de Lobby para las vistas "Crear Lobby" y "Mis Lobbys" */
@Data
@Builder
public class LobbyResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private LobbyType lobbyType;
    private LobbyPrivacy privacy;
    private Short maxMembers;
    private long memberCount; // Conteo actual de miembros
    private List<String> tags;
    private String gameName;
    private String ownerUsername;
    private OffsetDateTime createdAt;
}
