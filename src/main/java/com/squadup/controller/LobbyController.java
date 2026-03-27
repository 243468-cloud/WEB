package com.squadup.controller;

import com.squadup.dto.LobbyRequest;
import com.squadup.dto.LobbyResponse;
import com.squadup.service.LobbyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador de Lobbies.
 * Vistas: Crear Lobby, Mis Lobbys, Editar Lobby.
 */
@RestController
@RequestMapping("/api/lobbies")
@RequiredArgsConstructor
public class LobbyController {

    private final LobbyService lobbyService;
    private final com.squadup.repository.UserRepository userRepo;

    /** POST /api/lobbies → Crear Lobby */
    @PostMapping
    public ResponseEntity<LobbyResponse> create(
            @Valid @RequestBody LobbyRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        Long userId = resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(lobbyService.create(req, userId));
    }

    /** GET /api/lobbies/my → Mis Lobbys (Tab "Mis Lobbys") */
    @GetMapping("/my")
    public ResponseEntity<List<LobbyResponse>> myLobbies(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(lobbyService.getOwnedLobbies(resolveUserId(principal)));
    }

    /** GET /api/lobbies/joined → Mis Lobbys (Tab "Unido") */
    @GetMapping("/joined")
    public ResponseEntity<List<LobbyResponse>> joinedLobbies(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(lobbyService.getJoinedLobbies(resolveUserId(principal)));
    }

    /** PUT /api/lobbies/{id} → Editar Lobby */
    @PutMapping("/{id}")
    public ResponseEntity<LobbyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody LobbyRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(lobbyService.update(id, req, resolveUserId(principal)));
    }

    /** DELETE /api/lobbies/{id} → Eliminar Lobby */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        lobbyService.delete(id, resolveUserId(principal));
        return ResponseEntity.noContent().build();
    }

    /** POST /api/lobbies/{id}/join → Solicitar unirse */
    @PostMapping("/{id}/join")
    public ResponseEntity<Map<String, String>> join(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        String msg = lobbyService.requestJoin(id, resolveUserId(principal));
        return ResponseEntity.ok(Map.of("message", msg));
    }

    /** POST /api/lobbies/{id}/leave → Abandonar lobby */
    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leave(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        lobbyService.leave(id, resolveUserId(principal));
        return ResponseEntity.noContent().build();
    }

    /** PATCH /api/lobbies/requests/{requestId} → Aceptar / rechazar ✔ ✘ */
    @PatchMapping("/requests/{requestId}")
    public ResponseEntity<Void> reviewRequest(
            @PathVariable Long requestId,
            @RequestParam boolean accept,
            @AuthenticationPrincipal UserDetails principal) {
        lobbyService.reviewJoinRequest(requestId, accept, resolveUserId(principal));
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(UserDetails principal) {
        return userRepo.findByEmail(principal.getUsername())
                .orElseThrow().getId();
    }
}
