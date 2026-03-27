package com.squadup.repository;

import com.squadup.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /** Posts activos (no borrados) de un lobby, ordenados por fecha */
    @Query("SELECT p FROM Post p WHERE p.lobby.id = :lobbyId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<Post> findActiveByLobbyId(@Param("lobbyId") Long lobbyId);

    /** Publicaciones guardadas por el usuario */
    @Query("SELECT p FROM Post p JOIN SavedPost sp ON sp.post.id = p.id WHERE sp.user.id = :userId AND p.deletedAt IS NULL ORDER BY sp.savedAt DESC")
    List<Post> findSavedByUserId(@Param("userId") Long userId);

    /** Propias publicaciones activas del usuario */
    @Query("SELECT p FROM Post p WHERE p.author.id = :userId AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<Post> findActiveByAuthorId(@Param("userId") Long userId);
}
