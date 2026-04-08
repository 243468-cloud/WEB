package com.squadup.entity;

import com.squadup.entity.enums.MessageStatus;
import com.squadup.entity.enums.MessageType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
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
 * Mensaje de chat en tiempo real dentro de un lobby.
 * Soporta: chat grupal de lobby Y DM privado 1:1.
 * Vista: campo "Mensaje" + hilo del lobby; chat privado al aceptar solicitud.
 *
 * lobby_id = NULL → DM privado entre dos usuarios
 * recipient_id = NULL → mensaje de lobby
 */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** NULL si es DM privado */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lobby_id", foreignKey = @ForeignKey(name = "fk_msg_lobby", value = ConstraintMode.CONSTRAINT))
    private Lobby lobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", foreignKey = @ForeignKey(name = "fk_msg_sender", value = ConstraintMode.CONSTRAINT))
    private User sender;

    /** NULL si es mensaje de lobby */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", foreignKey = @ForeignKey(name = "fk_msg_recipient", value = ConstraintMode.CONSTRAINT))
    private User recipient;

    /** Autorreferencia para hilos de respuesta */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id", foreignKey = @ForeignKey(name = "fk_msg_reply", value = ConstraintMode.CONSTRAINT))
    private Message replyTo;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "msg_type", nullable = false)
    @Builder.Default
    private MessageType msgType = MessageType.TEXT;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    /** JSON — adjuntos: {url, filename, size, width, height} */
    @Type(JsonType.class)
    @Column(name = "attachment", columnDefinition = "json")
    private Map<String, Object> attachment;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private OffsetDateTime sentAt;

    @Column(name = "edited_at", columnDefinition = "DATETIME(6)")
    private OffsetDateTime editedAt;

    /** Soft-delete */
    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    private OffsetDateTime deletedAt;
}
