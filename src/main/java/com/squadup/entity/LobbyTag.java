package com.squadup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad LobbyTag — tabla `lobby_tags`.
 * Reemplaza la columna JSON `tags` de la tabla `lobbies` (violaba 1FN).
 * Cada fila representa un tag único por lobby.
 */
@Entity
@Table(
    name = "lobby_tags",
    uniqueConstraints = @UniqueConstraint(name = "uq_lobby_tag", columnNames = {"lobby_id", "tag"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LobbyTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lobby_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lt_lobby"))
    private Lobby lobby;

    @Column(name = "tag", nullable = false, length = 50)
    private String tag;
}