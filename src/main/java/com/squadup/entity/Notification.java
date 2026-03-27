package com.squadup.entity;

import com.squadup.entity.enums.NotificationType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Notificación en tiempo real.
 * Vista: Campana ; panel "Notificaciones".
 * WebSocket: al persistir, el service emite un evento al canal del recipient.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notif_recipient"))
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", foreignKey = @ForeignKey(name = "fk_notif_actor"))
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "notif_type", nullable = false)
    private NotificationType notifType;

    /** JSONB nativo PostgreSQL — Hibernate 6 API (@Type con clase directa) */
    @Type(JsonBinaryType.class)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private OffsetDateTime createdAt;

    @Column(name = "read_at", columnDefinition = "DATETIME(6)")
    private OffsetDateTime readAt;
}
