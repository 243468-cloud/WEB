package com.squadup.repository;

import com.squadup.entity.LobbyJoinRequest;
import com.squadup.entity.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LobbyJoinRequestRepository extends JpaRepository<LobbyJoinRequest, Long> {

    Optional<LobbyJoinRequest> findByLobbyIdAndRequesterIdAndStatus(
            Long lobbyId, Long requesterId, JoinRequestStatus status);

    List<LobbyJoinRequest> findByLobbyIdAndStatus(Long lobbyId, JoinRequestStatus status);

    boolean existsByLobbyIdAndRequesterIdAndStatus(
            Long lobbyId, Long requesterId, JoinRequestStatus status);
}
