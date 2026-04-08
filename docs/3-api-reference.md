# 3. Referencia Completa de la API REST

## 3.1 Información General

| Propiedad         | Valor                                    |
|-------------------|------------------------------------------|
| URL Base (local)  | `http://localhost:8080`                  |
| Prefijo API       | `/api`                                   |
| Content-Type      | `application/json`                       |
| Autenticación     | JWT Bearer Token en header `Authorization` |
| Puerto por defecto| 8080 (configurable vía `server.port`)    |

### Autenticación en endpoints protegidos
Todo endpoint que no sea público requiere el header:
```http
Authorization: Bearer <token_jwt>
```

---

## 3.2 Manejo Global de Errores

El sistema utiliza `@RestControllerAdvice` (`GlobalExceptionHandler`) para producir respuestas de error uniformes.

### Error estándar de dominio (404, 400, 403):
```json
{
  "status": 404,
  "message": "Lobby no encontrado",
  "timestamp": "2026-04-07T20:31:55.294-06:00"
}
```

### Error de validación Bean Validation (400):
```json
{
  "status": 400,
  "errors": {
    "fullName": "El nombre completo es obligatorio",
    "confirmPassword": "Confirma tu contraseña",
    "username": "El apodo debe tener entre 3 y 50 caracteres"
  },
  "timestamp": "2026-04-07T20:31:55.294-06:00"
}
```

---

## 3.3 AuthController — Autenticación (`/api/auth`)

### `POST /api/auth/register` — Crear Cuenta
**Protección:** Pública (no requiere token)

**Request Body (`RegisterRequest`):**

| Campo             | Tipo     | Requerido | Validaciones                                            |
|-------------------|----------|-----------|----------------------------------------------------------|
| `fullName`        | String   | ✅         | No vacío. Debe contener al menos una letra.              |
| `username`        | String   | ✅         | Min 3, Max 50 chars. Debe contener al menos una letra.   |
| `email`           | String   | ✅         | Formato `@Email` válido.                                 |
| `password`        | String   | ✅         | Min 8 caracteres.                                        |
| `confirmPassword` | String   | ✅         | Debe coincidir con `password`.                           |

**Ejemplo:**
```json
{
  "fullName": "Juan García",
  "username": "gamer01",
  "email": "juan@mail.com",
  "password": "Password123!",
  "confirmPassword": "Password123!"
}
```

**Respuesta exitosa (`201 Created`) — `AuthResponse`:**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "gamer01",
  "email": "juan@mail.com",
  "avatarUrl": null
}
```

**Posibles errores:**

| HTTP    | Mensaje                             | Causa                              |
|---------|--------------------------------------|------------------------------------|
| `400`   | `Las contraseñas no coinciden`      | password ≠ confirmPassword         |
| `400`   | `El correo ya está registrado`      | Email duplicado en BD              |
| `400`   | `El apodo ya está en uso`           | Username duplicado en BD           |
| `400`   | Bean validation errors (mapa)       | Campos faltantes o inválidos       |

---

### `POST /api/auth/login` — Iniciar Sesión
**Protección:** Pública (no requiere token)

**Request Body (`LoginRequest`):**

| Campo      | Tipo   | Requerido | Validaciones             |
|------------|--------|-----------|--------------------------|
| `email`    | String | ✅         | Formato `@Email` válido. |
| `password` | String | ✅         | No vacío.                |

**Ejemplo:**
```json
{
  "email": "juan@mail.com",
  "password": "Password123!"
}
```

**Respuesta exitosa (`200 OK`) — `AuthResponse`:**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "username": "gamer01",
  "email": "juan@mail.com",
  "avatarUrl": null
}
```

**Posibles errores:**

| HTTP    | Mensaje                     | Causa                        |
|---------|-----------------------------|------------------------------|
| `404`   | `Usuario no encontrado`    | Email no existe en BD         |
| `400`   | `Credenciales inválidas`   | Contraseña incorrecta         |

---

## 3.4 LobbyController — Gestión de Lobbies (`/api/lobbies`)

> **Todas estas rutas requieren** `Authorization: Bearer <token>`

### `POST /api/lobbies` — Crear Lobby

**Request Body (`LobbyRequest`):**

| Campo         | Tipo          | Requerido | Validaciones                          | Default     |
|---------------|---------------|-----------|---------------------------------------|-------------|
| `name`        | String        | ✅         | No vacío. Max 100 chars.             | —           |
| `description` | String        | ❌         | Max 500 chars.                       | `null`      |
| `gameId`      | Long          | ❌         | ID de un Game existente.             | `null`      |
| `lobbyType`   | Enum(String)  | ❌         | `CASUAL`, `COMPETITIVE`, `RANKED`    | `CASUAL`    |
| `privacy`     | Enum(String)  | ❌         | `PUBLIC`, `PRIVATE`                  | `PUBLIC`    |
| `maxMembers`  | Short         | ❌         | Min 2, Max 500.                      | `10`        |
| `tags`        | List\<String\>| ❌         | Lista de etiquetas de texto.         | `null`      |

**Ejemplo:**
```json
{
  "name": "Equipo Valorant Inmortal",
  "description": "Solo jugadores rango inmortal+",
  "gameId": 1,
  "lobbyType": "RANKED",
  "privacy": "PRIVATE",
  "maxMembers": 5,
  "tags": ["valorant", "inmortal", "latam"]
}
```

**Respuesta exitosa (`201 Created`) — `LobbyResponse`:**
```json
{
  "id": 1,
  "name": "Equipo Valorant Inmortal",
  "description": "Solo jugadores rango inmortal+",
  "imageUrl": null,
  "lobbyType": "RANKED",
  "privacy": "PRIVATE",
  "maxMembers": 5,
  "memberCount": 1,
  "tags": ["valorant", "inmortal", "latam"],
  "gameName": "Valorant",
  "ownerUsername": "gamer01",
  "createdAt": "2026-04-07T20:35:00-06:00"
}
```

**Comportamiento interno:** El creador se inserta automáticamente como `LobbyMember` con rol `OWNER`.

---

### `GET /api/lobbies/my` — Mis Lobbies (como dueño)

**Respuesta:** `200 OK` — Array de `LobbyResponse`

---

### `GET /api/lobbies/joined` — Lobbies donde soy miembro

**Respuesta:** `200 OK` — Array de `LobbyResponse`

---

### `PUT /api/lobbies/{id}` — Editar Lobby

**Path Params:** `id` (Long) — ID del lobby.
**Body:** Mismo que `POST /api/lobbies` (campos parciales actualizan).
**Permisos:** Solo OWNER o ADMIN del lobby.
**Respuesta:** `200 OK` — `LobbyResponse` actualizado.

| HTTP  | Mensaje                                 |
|-------|-----------------------------------------|
| `403` | `No eres miembro de este lobby`        |
| `403` | `No tienes permisos en este lobby`     |
| `404` | `Lobby no encontrado`                  |

---

### `DELETE /api/lobbies/{id}` — Eliminar Lobby (soft-delete)

**Permisos:** Solo OWNER.
**Respuesta:** `204 No Content`
**Comportamiento:** Pone `is_active = false` (no elimina registros).

---

### `POST /api/lobbies/{id}/join` — Unirse o solicitar entrada

**Respuesta:** `200 OK` — `{ "message": "..." }`

**Lógica condicional:**
- **Lobby PUBLIC** → El usuario entra directamente como MEMBER. Se notifica a los miembros actuales.
- **Lobby PRIVATE** → Se crea un `LobbyJoinRequest` con status `PENDING`. Se notifica al owner.

| HTTP  | Mensaje                                 |
|-------|-----------------------------------------|
| `400` | `Ya eres miembro de este lobby`        |
| `400` | `El lobby está lleno`                  |
| `400` | `Ya tienes una solicitud pendiente`    |

---

### `POST /api/lobbies/{id}/leave` — Abandonar Lobby

**Respuesta:** `204 No Content`

| HTTP  | Mensaje                                        |
|-------|------------------------------------------------|
| `400` | `El dueño no puede abandonar; elimina el lobby`|
| `404` | `No eres miembro de este lobby`               |

---

### `PATCH /api/lobbies/requests/{requestId}?accept=true|false` — Revisar solicitud

**Query Params:** `accept` (boolean) — `true` para aceptar, `false` para rechazar.
**Permisos:** Solo OWNER o ADMIN del lobby asociado.
**Respuesta:** `204 No Content`
**Comportamiento:** Si `accept=true`, el solicitante se inserta como MEMBER y recibe notificación de aceptación. Si `accept=false`, recibe notificación de rechazo.

---

## 3.5 NotificationController — Notificaciones (`/api/notifications`)

> **Todas estas rutas requieren** `Authorization: Bearer <token>`

### `GET /api/notifications` — Últimas 30 notificaciones

**Respuesta exitosa (`200 OK`):** Array de `NotificationResponse`:
```json
[
  {
    "id": 5,
    "type": "JOIN_REQUEST",
    "actorUsername": "player42",
    "actorAvatar": "https://...",
    "payload": {
      "lobbyId": 1,
      "lobbyName": "Equipo Valorant",
      "requesterId": 3
    },
    "read": false,
    "createdAt": "2026-04-07T20:40:00-06:00"
  }
]
```

---

### `GET /api/notifications/unread-count` — Contador de no leídas

**Respuesta:** `200 OK`
```json
{
  "count": 7
}
```

---

### `PATCH /api/notifications/read-all` — Marcar todas como leídas

**Respuesta:** `204 No Content`

---

## 3.6 Resumen de Rutas

| Método   | Ruta                                       | Protección    | Respuesta    |
|----------|--------------------------------------------|---------------|--------------|
| `POST`   | `/api/auth/register`                       | Pública       | `201`        |
| `POST`   | `/api/auth/login`                          | Pública       | `200`        |
| `POST`   | `/api/lobbies`                             | JWT           | `201`        |
| `GET`    | `/api/lobbies/my`                          | JWT           | `200`        |
| `GET`    | `/api/lobbies/joined`                      | JWT           | `200`        |
| `PUT`    | `/api/lobbies/{id}`                        | JWT + OWNER/ADMIN | `200`   |
| `DELETE` | `/api/lobbies/{id}`                        | JWT + OWNER   | `204`        |
| `POST`   | `/api/lobbies/{id}/join`                   | JWT           | `200`        |
| `POST`   | `/api/lobbies/{id}/leave`                  | JWT           | `204`        |
| `PATCH`  | `/api/lobbies/requests/{requestId}`        | JWT + OWNER/ADMIN | `204`   |
| `GET`    | `/api/notifications`                       | JWT           | `200`        |
| `GET`    | `/api/notifications/unread-count`          | JWT           | `200`        |
| `PATCH`  | `/api/notifications/read-all`              | JWT           | `204`        |
