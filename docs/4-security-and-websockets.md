# 4. Seguridad (JWT) y WebSockets (STOMP)

## 4.1 Arquitectura de Seguridad (Spring Security)

El sistema de seguridad de SquadUp es completamente **stateless** (sin sesiones), lo que significa que el servidor no guarda el estado de los usuarios autenticados en memoria. Toda la seguridad está basada en **JSON Web Tokens (JWT)**.

### Configuración de CORS
Dado que el frontend (ej. Angular) y el backend pueden correr en dominios distintos, `SecurityConfig.java` implementa un filtro CORS robusto que procesa dinámicamente orígenes configurados en application properties (`app.cors.allowed-origins`).
- Expone explicitamente los headers requeridos, notablemente `Authorization`.

### Encriptación de Contraseñas
Se utiliza el algoritmo **BCrypt** de una vía (Hash) con nivel de fortaleza (strength) de 12 para almacenar los `password_hash` en BD.

---

## 4.2 Flujo Profundo de Autenticación JWT

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente (Frontend)
    participant A as AuthController
    participant S as AuthService
    participant R as UserRepository
    participant U as JwtUtil
    participant F as JwtAuthFilter

    C->>A: POST /auth/login {email, password}
    A->>S: authService.login(req)
    S->>R: findByEmail(email)
    R-->>S: User entity + password_hash
    
    S->>S: Valida: BCrypt.matches(password, hash)
    
    alt Credenciales Incorrectas
        S-->>C: 400 Bad Request (Credenciales inválidas)
    else Credenciales Correctas
        S->>U: generateToken(email)
        Note over U: jjwt 0.12.x<br/>Firma HMAC-SHA256
        U-->>S: JWT String "eyJhbGci..."
        S-->>A: AuthResponse DTO
        A-->>C: 200 OK + Token
    end

    Note over C, F: Petición a ruta protegida (ej: GET /lobbies/my)

    C->>F: Request con header "Authorization: Bearer eyJhb..."
    F->>F: Extrae String después de "Bearer "
    F->>U: isValid(token) → parseClaims()
    
    alt Token Inválido o Expirado
        U-->>F: JwtException / False
        F-->>C: Cadena denegada → 403 Forbidden
    else Token Válido
        U-->>F: True
        F->>U: extractEmail(token)
        U-->>F: "user@mail.com"
        F->>R: loadUserByUsername(email)
        F->>F: UsernamePasswordAuthenticationToken(...)
        F->>F: SecurityContextHolder.setAuthentication(auth)
        F->>C: Permite paso al Controller
    end
```

### Detalles de Implementación de JWT (`jjwt 0.12.x`)
- Se inyecta la clave secreta `app.jwt.secret` y se inicializa mediante `Keys.hmacShaKeyFor()`.
- El payload solo transporta el campo estandarizado `subject` que apuntamos al correo electrónico. Toda la carga pesada de roles se carga del lado del servidor tras la validación de la firma en cada petición.

---

## 4.3 WebSockets: Comunicación Asíncrona (STOMP)

Para cumplir con la hiperactividad de un LFG, el proyecto expone un servidor WebSocket envolvente bajo el subprotocolo STOMP (Simple Text Oriented Messaging Protocol).

### Setup del EndPoint (`WebSocketConfig.java`)
- **Punto de Conexión:** `/ws`
- **Fallback:** Soporte nativo de `SockJS` por el cliente en navegadores donde no se soporta WS de forma prístina.
- **Canales de envío cliente-servidor:** Prefijo `/app` (ej. `/app/lobby/1/send`).
- **Brokers de retransmisión servidor-cliente:**
    - `/topic` para difusión pública o 1:N (un mensaje a todo un grupo).
    - `/user` para difusión privada 1:1 o eventos push exclusivos.

---

## 4.4 Flujo STOMP: Chat de Lobby y Notificaciones

Cuando dos usuarios están en el mismo lobby (Id `1`), la plataforma coordina los mensajes en tiempo real.

```mermaid
sequenceDiagram
    participant UserA as User A (Frontend)
    participant Broker as Spring Broker (/topic)
    participant WS as ChatWebSocketController
    participant DB as MessageRepository
    participant NotifSvc as NotificationService
    participant Queue as Spring User Queue (/user)
    participant UserB as User B (Frontend)

    Note over UserA, UserB: Ambos usuarios se conectan a /ws
    UserA->>Broker: SUBSCRIBE /topic/lobby/1
    UserB->>Broker: SUBSCRIBE /topic/lobby/1

    UserA->>WS: SEND /app/lobby/1/send {"content":"hola", "type":"TEXT"}
    
    WS->>WS: validateMembership(User A, Lobby 1)
    
    WS->>DB: save(MessageEntity)
    
    WS->>Broker: messaging.convertAndSend("/topic/lobby/1", msgMap)
    Broker-->>UserA: BROADCAST mensaje renderizado
    Broker-->>UserB: BROADCAST mensaje renderizado

    Note over UserA, Queue: ¿Qué pasa si "User A" envía una imagen?
    
    UserA->>WS: SEND /app/lobby/1/send {"content":"url", "type":"IMAGE"}
    WS->>DB: save(MessageEntity)
    WS->>Broker: BROADCAST al canal
    WS->>NotifSvc: notifyImageSent(lobby, User A, msg)
    
    NotifSvc->>NotifSvc: Identifica a User B (miembro que no es sender)
    NotifSvc->>Queue: convertAndSendToUser(User B, "/queue/notify", DTO)
    Queue-->>UserB: Push silencioso de "Nueva Imagen 🖼️"
```

### Mensajes Directos (DM)
También se soporta chat 1 a 1 mediante este mismo protocolo.
- Ruta de envío: `/app/dm/{recipientId}/send`.
- El Broker no lo manda a un public topic, sino a una queue privada interceptada en tiempo real mediante `convertAndSendToUser(recipientId, "/queue/dm", payload)`.

### Eventos Push Globales (Campanita)
Cualquier clase en Spring puede inyectar el componente `NotificationService`. Cuando ocurre un evento relacional que demanda la atención subita de otro jugador (por ejemplo en `LobbyService.reviewJoinRequest`), el servidor inmediatamente lo empuja por socket enviándolo a la ruta `/user/{id}/queue/notify`.
