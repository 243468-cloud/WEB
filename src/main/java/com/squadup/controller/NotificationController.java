package com.squadup.controller;

import com.squadup.dto.NotificationResponse;
import com.squadup.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador de Notificaciones.
 * Vista: Panel de la campana 🔔 y lista de alertas.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifService;
    private final com.squadup.repository.UserRepository userRepo;

    /** GET /api/notifications → Últimas 30 notificaciones */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(notifService.getRecent(resolveUserId(principal)));
    }

    /** GET /api/notifications/unread-count → Badge 🔔 */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal UserDetails principal) {
        long count = notifService.countUnread(resolveUserId(principal));
        return ResponseEntity.ok(Map.of("count", count));
    }

    /** PATCH /api/notifications/read-all → Marcar todas como leídas */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal UserDetails principal) {
        notifService.markAllRead(resolveUserId(principal));
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(UserDetails principal) {
        return userRepo.findByEmail(principal.getUsername()).orElseThrow().getId();
    }
}
