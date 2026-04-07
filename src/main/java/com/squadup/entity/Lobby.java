package com.squadup.entity;

import com.squadup.entity.enums.LobbyPrivacy;
import com.squadup.entity.enums.LobbyType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Entidad Lobby — tabla `lobbies`.
 * Vistas: Crear Lobby, Mis Lobbys, Editar Lobby, Panel de Perfil.
 *
 * Tipos JSON con MySQL 8+ (Hibernate 6 / hypersistence-utils):
 * @Type(JsonType.class) → JSON  (columna tipo JSON nativo de MySQL)
 */
@Entity
@Table(name = "lobbies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lobby_owner"))
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", foreignKey = @ForeignKey(name = "fk_lobby_game"))
    private Game game;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "lobby_type", nullable = false)
    @Builder.Default
    private LobbyType lobbyType = LobbyType.CASUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy", nullable = false)
    @Builder.Default
    private LobbyPrivacy privacy = LobbyPrivacy.PUBLIC;

    @Column(name = "max_members", nullable = false)
    @Builder.Default
    private Short maxMembers = 10;

    /**
     * Tags del lobby — reemplaza la columna JSON tags (violaba 1FN).
     * Mapeado a la tabla lobby_tags con cascada completa.
     * Para leer los valores como List<String> usar getTags().
     */
    @OneToMany(mappedBy = "lobby", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LobbyTag> lobbyTags = new ArrayList<>();

    /**
     * Retorna los tags como List<String> para compatibilidad con DTOs y LobbyService.
     */
    public List<String> getTags() {
        return lobbyTags.stream().map(LobbyTag::getTag).toList();
    }

    /**
     * Reemplaza todos los tags del lobby. Limpia la lista existente y
     * agrega los nuevos respetando el orphanRemoval de la relación.
     */
    public void setTags(List<String> tags) {
        this.lobbyTags.clear();
        if (tags != null) {
            tags.stream()
                .distinct()
                .map(t -> LobbyTag.builder().lobby(this).tag(t).build())
                .forEach(this.lobbyTags::add);
        }
    }

    /** JSON genérico para meta_data soportado por MySQL */
    @Type(JsonType.class)
    @Column(name = "extra_meta", columnDefinition = "json")
    private Map<String, Object> extraMeta;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME")
    private OffsetDateTime updatedAt;

    // ── Relaciones ────────────────────────────
    @OneToMany(mappedBy = "lobby", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LobbyMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "lobby", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LobbyJoinRequest> joinRequests = new ArrayList<>();

    @OneToMany(mappedBy = "lobby", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "lobby", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
}