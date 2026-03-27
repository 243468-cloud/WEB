package com.squadup.controller;

import com.squadup.entity.Lobby;
import com.squadup.entity.Message;
import com.squadup.entity.User;
import com.squadup.entity.enums.MessageType;
import com.squadup.exception.ForbiddenException;
import com.squadup.exception.ResourceNotFoundException;
import com.squadup.repository.LobbyMemberRepository;
import com.squadup.repository.LobbyRepository;
import com.squadup.repository.MessageRepository;
import com.squadup.repository.UserRepository;
import com.squadup.service.NotificationService;
import lombok.*;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * Controlador WebSocket STOMP para el chat de lobbies y DMs privados.
 *
 * Canales:
 * Cliente envía a → /app/lobby/{lobbyId}/send
 * Servidor publica → /topic/lobby/{lobbyId}
 *
 * DM:
 * Cliente envía a → /app/dm/{recipientId}/send
 * Servidor publica → /user/{recipientId}/queue/dm
 */
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

        private final MessageRepository messageRepo;
        private final LobbyRepository lobbyRepo;
        private final LobbyMemberRepository memberRepo;
        private final UserRepository userRepo;
        private final NotificationService notificationService;
        private final SimpMessagingTemplate messaging;

        /** Mensaje en el chat del lobby */
        @MessageMapping("/lobby/{lobbyId}/send")
        public void sendLobbyMessage(@DestinationVariable Long lobbyId,
                        @Payload ChatMessage payload,
                        Principal principal) {
                User sender = resolveUser(principal);
                Lobby lobby = lobbyRepo.findById(lobbyId)
                                .orElseThrow(() -> new ResourceNotFoundException("Lobby no encontrado"));

                if (!memberRepo.existsByLobbyIdAndUserId(lobbyId, sender.getId()))
                        throw new ForbiddenException("No eres miembro de este lobby");

                MessageType type = MessageType.valueOf(
                                payload.getType() != null ? payload.getType().toUpperCase() : "TEXT");

                Message msg = Message.builder()
                                .lobby(lobby).sender(sender)
                                .content(payload.getContent()).msgType(type)
                                .build();
                messageRepo.save(msg);

                // Notificación si es imagen
                if (type == MessageType.IMAGE)
                        notificationService.notifyImageSent(lobby, sender, msg);

                // Broadcast al canal del lobby
                messaging.convertAndSend("/topic/lobby/" + lobbyId, Map.of(
                                "id", msg.getId(),
                                "senderId", sender.getId(),
                                "username", sender.getUsername(),
                                "avatar", sender.getAvatarUrl(),
                                "content", msg.getContent() != null ? msg.getContent() : "",
                                "type", type.name(),
                                "sentAt", msg.getSentAt().toString()));
        }

        /** Mensaje privado DM */
        @MessageMapping("/dm/{recipientId}/send")
        public void sendDm(@DestinationVariable Long recipientId,
                        @Payload ChatMessage payload,
                        Principal principal) {
                User sender = resolveUser(principal);
                User recipient = userRepo.findById(recipientId)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

                Message msg = Message.builder()
                                .sender(sender).recipient(recipient)
                                .content(payload.getContent()).msgType(MessageType.TEXT)
                                .build();
                messageRepo.save(msg);

                // Mensaje al destinatario
                messaging.convertAndSendToUser(recipientId.toString(), "/queue/dm", Map.of(
                                "id", msg.getId(),
                                "senderId", sender.getId(),
                                "username", sender.getUsername(),
                                "content", msg.getContent(),
                                "sentAt", msg.getSentAt().toString()));
        }

        private User resolveUser(Principal principal) {
                return userRepo.findByEmail(principal.getName())
                                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        }

        @Data
        public static class ChatMessage {
                private String content;
                private String type; // "text" | "image" | "gif"
        }
}
