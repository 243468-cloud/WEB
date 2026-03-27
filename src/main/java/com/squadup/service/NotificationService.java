package com.squadup.service;

import com.squadup.dto.NotificationResponse;
import com.squadup.entity.*;
import com.squadup.entity.enums.NotificationType;
import com.squadup.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Servicio de Notificaciones:
 * - Persiste la notificación en la BD
 * - Empuja el evento por WebSocket al canal personal del usuario
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifRepo;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket

    /** Conteo de no leídas — Badge de la campana 🔔 */
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notifRepo.countByRecipientIdAndIsReadFalse(userId);
    }

    /** Últimas 30 notificaciones */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecent(Long userId) {
        return notifRepo.findTop30ByRecipientIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    /** Marcar todas como leídas */
    @Transactional
    public void markAllRead(Long userId) {
        notifRepo.markAllReadByUserId(userId);
    }

    // ── Eventos del sistema → generan notificación + push WS ──────────────

    public void notifyJoinRequest(Lobby lobby, User requester) {
        persist(lobby.getOwner(), requester, NotificationType.JOIN_REQUEST,
                Map.of("lobbyId", lobby.getId(),
                        "lobbyName", lobby.getName(),
                        "requesterId", requester.getId()));
    }

    public void notifyRequestAccepted(Lobby lobby, User user) {
        persist(user, null, NotificationType.REQUEST_ACCEPTED,
                Map.of("lobbyId", lobby.getId(), "lobbyName", lobby.getName()));
    }

    public void notifyRequestRejected(Lobby lobby, User user) {
        persist(user, null, NotificationType.REQUEST_REJECTED,
                Map.of("lobbyId", lobby.getId(), "lobbyName", lobby.getName()));
    }

    public void notifyUserJoined(Lobby lobby, User user) {
        lobby.getMembers().forEach(m -> {
            if (!m.getUser().getId().equals(user.getId()))
                persist(m.getUser(), user, NotificationType.USER_JOINED,
                        Map.of("lobbyId", lobby.getId(), "username", user.getUsername()));
        });
    }

    public void notifyUserLeft(Lobby lobby, User user) {
        lobby.getMembers().forEach(m -> persist(m.getUser(), user, NotificationType.USER_LEFT,
                Map.of("lobbyId", lobby.getId(), "username", user.getUsername())));
    }

    public void notifyMention(User recipient, User actor, Post post) {
        persist(recipient, actor, NotificationType.MENTION,
                Map.of("postId", post.getId(), "lobbyId", post.getLobby().getId()));
    }

    public void notifyImageSent(Lobby lobby, User sender, Message message) {
        lobby.getMembers().stream()
                .filter(m -> !m.getUser().getId().equals(sender.getId()))
                .forEach(m -> persist(m.getUser(), sender, NotificationType.IMAGE_SENT,
                        Map.of("messageId", message.getId(), "lobbyId", lobby.getId())));
    }

    // ── Helper: persiste y hace push WebSocket ─────────────────────────────

    @Transactional
    protected void persist(User recipient, User actor,
            NotificationType type, Map<String, Object> payload) {
        Notification notif = Notification.builder()
                .recipient(recipient).actor(actor)
                .notifType(type).payload(payload)
                .build();
        notifRepo.save(notif);

        // Push en tiempo real al canal personal del usuario
        messagingTemplate.convertAndSendToUser(
                recipient.getId().toString(),
                "/queue/notify",
                toDto(notif));
    }

    private NotificationResponse toDto(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).type(n.getNotifType())
                .actorUsername(n.getActor() != null ? n.getActor().getUsername() : null)
                .actorAvatar(n.getActor() != null ? n.getActor().getAvatarUrl() : null)
                .payload(n.getPayload()).read(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
