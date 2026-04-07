package com.squadup.repository;

import com.squadup.entity.LobbyTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LobbyTagRepository extends JpaRepository<LobbyTag, Long> {

    List<LobbyTag> findByLobbyId(Long lobbyId);

    @Modifying
    @Query("DELETE FROM LobbyTag lt WHERE lt.lobby.id = :lobbyId")
    void deleteByLobbyId(@Param("lobbyId") Long lobbyId);

    @Query("SELECT lt.lobby.id FROM LobbyTag lt WHERE lt.tag = :tag")
    List<Long> findLobbyIdsByTag(@Param("tag") String tag);
}