package com.squadup.repository;

import com.squadup.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Historial de chat del lobby (paginado desde el controller) */
    @Query("SELECT m FROM Message m WHERE m.lobby.id = :lobbyId AND m.deletedAt IS NULL ORDER BY m.sentAt ASC")
    List<Message> findActiveLobbyMessages(@Param("lobbyId") Long lobbyId);

    /** DM privado entre dos usuarios */
    @Query("""
                SELECT m FROM Message m
                WHERE m.deletedAt IS NULL AND m.lobby IS NULL AND (
                    (m.sender.id = :a AND m.recipient.id = :b) OR
                    (m.sender.id = :b AND m.recipient.id = :a)
                ) ORDER BY m.sentAt ASC
            """)
    List<Message> findDirectMessages(@Param("a") Long userA, @Param("b") Long userB);
}
