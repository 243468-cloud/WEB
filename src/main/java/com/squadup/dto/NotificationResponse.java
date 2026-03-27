package com.squadup.dto;

import com.squadup.entity.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

/** DTO de notificación para el panel de alertas y eventos WebSocket */
@Data
@Builder
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String actorUsername;
    private String actorAvatar;
    private Map<String, Object> payload;
    private boolean read;
    private OffsetDateTime createdAt;
}
