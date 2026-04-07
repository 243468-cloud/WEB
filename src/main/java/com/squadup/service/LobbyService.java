package com.squadup.service;

import com.squadup.dto.LobbyRequest;
import com.squadup.dto.LobbyResponse;
import com.squadup.entity.*;
import com.squadup.entity.enums.JoinRequestStatus;
import com.squadup.entity.enums.MemberRole;
import com.squadup.exception.BadRequestException;
import com.squadup.exception.ForbiddenException;
import com.squadup.exception.ResourceNotFoundException;
import com.squadup.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LobbyService {

    private final LobbyRepository lobbyRepo;
    private final LobbyMemberRepository memberRepo;
    private final LobbyJoinRequestRepository joinReqRepo;
    private final GameRepository gameRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    /** Vista "Crear Lobby" */
    @Transactional
    public LobbyResponse create(LobbyRequest req, Long ownerId) {
        User owner = userRepo.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Game game = req.getGameId() != null
                ? gameRepo.findById(req.getGameId()).orElse(null)
                : null;

        Lobby lobby = Lobby.builder()
                .owner(owner).game(game)
                .name(req.getName()).description(req.getDescription())
                .lobbyType(req.getLobbyType()).privacy(req.getPrivacy())
                .maxMembers(req.getMaxMembers())
                .build();

        lobby.setTags(req.getTags());

        lobbyRepo.save(lobby);

        // El creador es automáticamente miembro con rol OWNER
        memberRepo.save(LobbyMember.builder()
                .lobby(lobby).user(owner).role(MemberRole.OWNER).build());

        return toResponse(lobby, 1L);
    }

    /** Vista "Mis Lobbys" — Tab "Mis Lobbys" */
    @Transactional(readOnly = true)
    public List<LobbyResponse> getOwnedLobbies(Long userId) {
        return lobbyRepo.findByOwnerIdAndIsActiveTrue(userId).stream()
                .map(l -> toResponse(l, memberRepo.countByLobbyId(l.getId())))
                .toList();
    }

    /** Vista "Mis Lobbys" — Tab "Unido" */
    @Transactional(readOnly = true)
    public List<LobbyResponse> getJoinedLobbies(Long userId) {
        return lobbyRepo.findJoinedLobbiesByUserId(userId).stream()
                .map(l -> toResponse(l, memberRepo.countByLobbyId(l.getId())))
                .toList();
    }

    /** Vista "Editar Lobby" */
    @Transactional
    public LobbyResponse update(Long lobbyId, LobbyRequest req, Long requesterId) {
        Lobby lobby = getOrThrow(lobbyId);
        checkOwnerOrAdmin(lobby, requesterId);

        lobby.setName(req.getName());
        lobby.setDescription(req.getDescription());
        lobby.setLobbyType(req.getLobbyType());
        lobby.setPrivacy(req.getPrivacy());
        lobby.setMaxMembers(req.getMaxMembers());
        lobby.setTags(req.getTags());
        if (req.getGameId() != null) {
            lobby.setGame(gameRepo.findById(req.getGameId()).orElse(null));
        }

        return toResponse(lobbyRepo.save(lobby), memberRepo.countByLobbyId(lobbyId));
    }

    /** Eliminar lobby (sólo owner) */
    @Transactional
    public void delete(Long lobbyId, Long requesterId) {
        Lobby lobby = getOrThrow(lobbyId);
        if (!lobby.getOwner().getId().equals(requesterId))
            throw new ForbiddenException("Solo el dueño puede eliminar el lobby");
        lobby.setIsActive(false);
        lobbyRepo.save(lobby);
    }

    /**
     * Solicitar unirse al lobby.
     * Si es público → ingresa directamente.
     * Si es privado → crea una solicitud y notifica al owner.
     */
    @Transactional
    public String requestJoin(Long lobbyId, Long userId) {
        Lobby lobby = getOrThrow(lobbyId);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (memberRepo.existsByLobbyIdAndUserId(lobbyId, userId))
            throw new BadRequestException("Ya eres miembro de este lobby");

        long currentCount = memberRepo.countByLobbyId(lobbyId);
        if (currentCount >= lobby.getMaxMembers())
            throw new BadRequestException("El lobby está lleno");

        if (lobby.getPrivacy() == com.squadup.entity.enums.LobbyPrivacy.PUBLIC) {
            memberRepo.save(LobbyMember.builder()
                    .lobby(lobby).user(user).role(MemberRole.MEMBER).build());
            notificationService.notifyUserJoined(lobby, user);
            return "Te has unido al lobby";
        } else {
            if (joinReqRepo.existsByLobbyIdAndRequesterIdAndStatus(
                    lobbyId, userId, JoinRequestStatus.PENDING))
                throw new BadRequestException("Ya tienes una solicitud pendiente");

            LobbyJoinRequest req = LobbyJoinRequest.builder()
                    .lobby(lobby).requester(user).build();
            joinReqRepo.save(req);
            notificationService.notifyJoinRequest(lobby, user);
            return "Solicitud enviada";
        }
    }

    /** Aceptar / rechazar solicitud — Notificación con ✔ ✘ */
    @Transactional
    public void reviewJoinRequest(Long requestId, boolean accept, Long reviewerId) {
        LobbyJoinRequest jr = joinReqRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        checkOwnerOrAdmin(jr.getLobby(), reviewerId);

        User reviewer = userRepo.findById(reviewerId).orElseThrow();
        jr.setReviewedBy(reviewer);
        jr.setReviewedAt(OffsetDateTime.now());
        jr.setStatus(accept ? JoinRequestStatus.ACCEPTED : JoinRequestStatus.REJECTED);
        joinReqRepo.save(jr);

        if (accept) {
            memberRepo.save(LobbyMember.builder()
                    .lobby(jr.getLobby()).user(jr.getRequester())
                    .role(MemberRole.MEMBER).build());
            notificationService.notifyRequestAccepted(jr.getLobby(), jr.getRequester());
        } else {
            notificationService.notifyRequestRejected(jr.getLobby(), jr.getRequester());
        }
    }

    /** Abandonar lobby */
    @Transactional
    public void leave(Long lobbyId, Long userId) {
        LobbyMember member = memberRepo.findByLobbyIdAndUserId(lobbyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No eres miembro de este lobby"));
        if (member.getRole() == MemberRole.OWNER)
            throw new BadRequestException("El dueño no puede abandonar; elimina el lobby");
        memberRepo.delete(member);
        Lobby lobby = getOrThrow(lobbyId);
        User user = userRepo.findById(userId).orElseThrow();
        notificationService.notifyUserLeft(lobby, user);
    }

    // ── Helpers ────────────────────────────────
    private Lobby getOrThrow(Long id) {
        return lobbyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lobby no encontrado"));
    }

    private void checkOwnerOrAdmin(Lobby lobby, Long userId) {
        LobbyMember member = memberRepo.findByLobbyIdAndUserId(lobby.getId(), userId)
                .orElseThrow(() -> new ForbiddenException("No eres miembro de este lobby"));
        if (member.getRole() == MemberRole.MEMBER)
            throw new ForbiddenException("No tienes permisos en este lobby");
    }

    private LobbyResponse toResponse(Lobby l, long count) {
        return LobbyResponse.builder()
                .id(l.getId()).name(l.getName()).description(l.getDescription())
                .imageUrl(l.getImageUrl()).lobbyType(l.getLobbyType())
                .privacy(l.getPrivacy()).maxMembers(l.getMaxMembers())
                .memberCount(count)
                .tags(l.getTags())
                .gameName(l.getGame() != null ? l.getGame().getName() : null)
                .ownerUsername(l.getOwner().getUsername())
                .createdAt(l.getCreatedAt())
                .build();
    }
}