package com.squadup.repository;

import com.squadup.entity.LobbyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LobbyMemberRepository extends JpaRepository<LobbyMember, Long> {
    Optional<LobbyMember> findByLobbyIdAndUserId(Long lobbyId, Long userId);

    List<LobbyMember> findByLobbyId(Long lobbyId);

    boolean existsByLobbyIdAndUserId(Long lobbyId, Long userId);

    long countByLobbyId(Long lobbyId);
}
