package com.squadup.entity;

import com.squadup.entity.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "oauth_accounts", uniqueConstraints = @UniqueConstraint(name = "uq_oauth_provider_uid", columnNames = {
        "provider", "provider_uid" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_oauth_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private OAuthProvider provider;

    @Column(name = "provider_uid", nullable = false, length = 255)
    private String providerUid;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at", columnDefinition = "DATETIME(6)")
    private OffsetDateTime tokenExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private OffsetDateTime createdAt;
}
