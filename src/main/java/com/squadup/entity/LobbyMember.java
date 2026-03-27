package com.squadup.entity;

import com.squadup.entity.enums.MemberRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "lobby_members", uniqueConstraints = @UniqueConstraint(name = "uq_lobby_members", columnNames = {
        "lobby_id", "user_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LobbyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lobby_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member_lobby"))
    private Lobby lobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private OffsetDateTime joinedAt;
}
