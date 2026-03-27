package com.squadup.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "saved_posts", uniqueConstraints = @UniqueConstraint(name = "uq_saved_posts", columnNames = { "user_id",
        "post_id" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_saved_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_saved_post"))
    private Post post;

    @CreationTimestamp
    @Column(name = "saved_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private OffsetDateTime savedAt;
}
