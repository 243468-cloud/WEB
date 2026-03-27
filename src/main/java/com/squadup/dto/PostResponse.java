package com.squadup.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/** DTO de respuesta de Post para el chat del lobby y "Mis Guardados" */
@Data
@Builder
public class PostResponse {
    private Long id;
    private String content;
    private boolean pinned;
    private boolean requiresApproval;
    private boolean savedByCurrentUser;
    private String authorUsername;
    private String authorAvatarUrl;
    private List<MediaItem> media;
    private OffsetDateTime createdAt;

    @Data
    @Builder
    public static class MediaItem {
        private String url;
        private String mediaType;
        private String filename;
    }
}
