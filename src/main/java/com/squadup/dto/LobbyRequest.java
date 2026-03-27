package com.squadup.dto;

import com.squadup.entity.enums.LobbyPrivacy;
import com.squadup.entity.enums.LobbyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** DTO para Crear / Editar Lobby */
@Data
public class LobbyRequest {

    @NotBlank(message = "El nombre de la comunidad es obligatorio")
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private Long gameId;

    private LobbyType lobbyType = LobbyType.CASUAL;

    private LobbyPrivacy privacy = LobbyPrivacy.PUBLIC;

    @Min(2)
    @Max(500)
    private Short maxMembers = 10;

    private List<String> tags;
}
