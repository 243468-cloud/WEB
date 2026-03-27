package com.squadup.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_games", uniqueConstraints = @UniqueConstraint(name = "uq_user_games", columnNames = { "user_id",
        "game_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ug_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ug_game"))
    private Game game;

    @Column(name = "rank", length = 20)
    private String rank;

    @Column(name = "rank_label", length = 50)
    private String rankLabel;

    @Column(name = "hours_played")
    private Integer hoursPlayed;

    @Column(name = "is_main", nullable = false)
    @Builder.Default
    private Boolean isMain = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private OffsetDateTime createdAt;
}
