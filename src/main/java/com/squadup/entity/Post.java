package com.squadup.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Publicación dentro de un lobby.
 * Vistas: Chat/Lobby → publicaciones; "Agregar Post"; "Mis Guardados".
 */
@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lobby_id", nullable = false, foreignKey = @ForeignKey(name = "fk_post_lobby"))
    private Lobby lobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "fk_post_author"))
    private User author;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "requires_approval", nullable = false)
    @Builder.Default
    private Boolean requiresApproval = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private OffsetDateTime updatedAt;

    /** Soft-delete: NULL = activo */
    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    private OffsetDateTime deletedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SavedPost> savedBy = new ArrayList<>();
}
