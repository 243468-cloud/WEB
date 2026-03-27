package com.squadup.entity;

import com.squadup.entity.enums.JoinRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Solicitudes de unión a lobby.
 * Vista: Notificación "Solicita unirse al grupo" + botones ✔ ✘
 */
@Entity
@Table(name = "lobby_join_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LobbyJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lobby_id", nullable = false, foreignKey = @ForeignKey(name = "fk_jr_lobby"))
    private Lobby lobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false, foreignKey = @ForeignKey(name = "fk_jr_requester"))
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", foreignKey = @ForeignKey(name = "fk_jr_reviewer"))
    private User reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private JoinRequestStatus status = JoinRequestStatus.PENDING;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private OffsetDateTime createdAt;

    @Column(name = "reviewed_at", columnDefinition = "DATETIME(6)")
    private OffsetDateTime reviewedAt;
}
