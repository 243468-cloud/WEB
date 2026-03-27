package com.squadup.repository;

import com.squadup.entity.Lobby;
import com.squadup.entity.enums.LobbyPrivacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LobbyRepository extends JpaRepository<Lobby, Long> {

    List<Lobby> findByOwnerIdAndIsActiveTrue(Long ownerId);

    List<Lobby> findByPrivacyAndIsActiveTrue(LobbyPrivacy privacy);

    @Query("SELECT l FROM Lobby l JOIN l.members m WHERE m.user.id = :userId AND l.isActive = true")
    List<Lobby> findJoinedLobbiesByUserId(@Param("userId") Long userId);

    @Query("SELECT l FROM Lobby l WHERE l.game.id = :gameId AND l.isActive = true")
    List<Lobby> findByGameId(@Param("gameId") Long gameId);
}
