# 2. Esquema de Base de Datos

## 2.1 Motor y Configuración

| Propiedad              | Valor                                      |
|------------------------|--------------------------------------------|
| Motor                  | MySQL 8.0+                                 |
| Charset                | utf8mb4                                    |
| Collation              | utf8mb4_0900_ai_ci                         |
| Storage Engine         | InnoDB (transaccional, soporte FK)         |
| Normalización          | Tercera Forma Normal (3NF)                 |
| DDL Strategy           | `spring.jpa.hibernate.ddl-auto=update`     |

---

## 2.2 Diagrama Entidad-Relación (ERD)

```mermaid
erDiagram
    users ||--o{ lobbies : "owns (owner_id)"
    users ||--o{ lobby_members : "is member"
    users ||--o{ lobby_join_requests : "requests entry"
    users ||--o{ messages : "sends"
    users ||--o{ notifications : "receives"
    users ||--o{ user_games : "plays"
    users ||--o{ posts : "authors"
    users ||--o{ saved_posts : "bookmarks"
    users ||--o{ oauth_accounts : "authenticates via"

    games ||--o{ user_games : "played by"
    games ||--o{ lobbies : "associated to"

    lobbies ||--o{ lobby_members : "has members"
    lobbies ||--o{ lobby_tags : "tagged with"
    lobbies ||--o{ lobby_join_requests : "receives requests"
    lobbies ||--o{ messages : "chat channel"
    lobbies ||--o{ posts : "content feed"

    posts ||--o{ post_media : "has attachments"
    posts ||--o{ saved_posts : "saved by users"

    messages ||--o{ messages : "reply_to"

    users {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR_50 username "UNIQUE, NOT NULL"
        VARCHAR_120 full_name "NOT NULL"
        VARCHAR_255 email "UNIQUE, NOT NULL"
        VARCHAR_255 password_hash "NULL si OAuth"
        TEXT avatar_url
        ENUM status "ACTIVE|BANNED|INACTIVE"
        DATETIME created_at "NOT NULL"
        DATETIME updated_at "NOT NULL"
        DATETIME last_login
    }

    games {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR_120 name "UNIQUE, NOT NULL"
        VARCHAR_30 genre "NOT NULL"
        TEXT cover_url
        TEXT description
        DATETIME6 created_at "NOT NULL"
    }

    user_games {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        BIGINT game_id FK "NOT NULL"
        VARCHAR_20 rank "ESCAPED con backticks"
        INT hours_played
        BIT is_main "DEFAULT false"
        DATETIME6 created_at "NOT NULL"
    }

    lobbies {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT owner_id FK "NOT NULL"
        BIGINT game_id FK
        VARCHAR_100 name "NOT NULL"
        TEXT description
        TEXT image_url
        ENUM lobby_type "COMPETITIVE|CASUAL|RANKED"
        ENUM privacy "PUBLIC|PRIVATE"
        SMALLINT max_members "NOT NULL, DEFAULT 10"
        JSON extra_meta
        BIT is_active "DEFAULT true"
        DATETIME created_at "NOT NULL"
        DATETIME updated_at "NOT NULL"
    }

    lobby_members {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT lobby_id FK "NOT NULL"
        BIGINT user_id FK "NOT NULL"
        ENUM role "OWNER|ADMIN|MEMBER"
        DATETIME6 joined_at "NOT NULL"
    }

    lobby_tags {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT lobby_id FK "NOT NULL"
        VARCHAR_50 tag "NOT NULL"
    }

    lobby_join_requests {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT lobby_id FK "NOT NULL"
        BIGINT requester_id FK "NOT NULL"
        BIGINT reviewed_by FK
        ENUM status "PENDING|ACCEPTED|REJECTED"
        TEXT message
        DATETIME6 created_at "NOT NULL"
        DATETIME6 reviewed_at
    }

    messages {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT sender_id FK
        BIGINT recipient_id FK "NULL si es lobby chat"
        BIGINT lobby_id FK "NULL si es DM"
        BIGINT reply_to_id FK "Self-reference"
        TEXT content
        ENUM msg_type "TEXT|IMAGE|GIF|SYSTEM"
        ENUM status "SENT|DELIVERED|READ|DELETED"
        JSON attachment
        DATETIME6 sent_at "NOT NULL"
        DATETIME6 edited_at
        DATETIME6 deleted_at
    }

    notifications {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT recipient_id FK "NOT NULL"
        BIGINT actor_id FK
        ENUM notif_type "JOIN_REQUEST|REQUEST_ACCEPTED|..."
        JSON payload
        BIT is_read "DEFAULT false"
        DATETIME6 created_at "NOT NULL"
        DATETIME6 read_at
    }

    posts {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT author_id FK "NOT NULL"
        BIGINT lobby_id FK "NOT NULL"
        TEXT content
        BIT is_pinned "DEFAULT false"
        BIT requires_approval "DEFAULT false"
        DATETIME6 created_at "NOT NULL"
        DATETIME6 updated_at "NOT NULL"
        DATETIME6 deleted_at
    }

    post_media {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT post_id FK "NOT NULL"
        TEXT url "NOT NULL"
        VARCHAR_10 media_type "NOT NULL"
        VARCHAR_255 filename
        BIGINT file_size
        INT sort_order "NOT NULL"
        DATETIME6 created_at "NOT NULL"
    }

    saved_posts {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        BIGINT post_id FK "NOT NULL"
        DATETIME6 saved_at "NOT NULL"
    }

    oauth_accounts {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        ENUM provider "GOOGLE|DISCORD"
        VARCHAR_255 provider_uid "NOT NULL"
        TEXT access_token
        TEXT refresh_token
        DATETIME6 token_expires_at
        DATETIME6 created_at "NOT NULL"
    }
```

---

## 2.3 Restricciones de Unicidad (Unique Constraints)

| Tabla               | Constraint Name          | Columnas                   |
|---------------------|--------------------------|----------------------------|
| `users`             | `uq_users_email`         | `email`                    |
| `users`             | `uq_users_username`      | `username`                 |
| `games`             | `uq_games_name`          | `name`                     |
| `user_games`        | `uq_user_games`          | `(user_id, game_id)`       |
| `lobby_members`     | `uq_lobby_members`       | `(lobby_id, user_id)`      |
| `lobby_tags`        | `uq_lobby_tag`           | `(lobby_id, tag)`          |
| `oauth_accounts`    | `uq_oauth_provider_uid`  | `(provider, provider_uid)` |
| `saved_posts`       | `uq_saved_posts`         | `(user_id, post_id)`       |

---

## 2.4 Claves Foráneas (Foreign Keys)

| FK Name              | Tabla Origen          | Columna         | Tabla Destino  |
|----------------------|-----------------------|-----------------|----------------|
| `fk_lobby_owner`     | `lobbies`             | `owner_id`      | `users`        |
| `fk_lobby_game`      | `lobbies`             | `game_id`       | `games`        |
| `fk_member_lobby`    | `lobby_members`       | `lobby_id`      | `lobbies`      |
| `fk_member_user`     | `lobby_members`       | `user_id`       | `users`        |
| `fk_jr_lobby`        | `lobby_join_requests` | `lobby_id`      | `lobbies`      |
| `fk_jr_requester`    | `lobby_join_requests` | `requester_id`  | `users`        |
| `fk_jr_reviewer`     | `lobby_join_requests` | `reviewed_by`   | `users`        |
| `fk_lt_lobby`        | `lobby_tags`          | `lobby_id`      | `lobbies`      |
| `fk_ug_user`         | `user_games`          | `user_id`       | `users`        |
| `fk_ug_game`         | `user_games`          | `game_id`       | `games`        |
| `fk_msg_sender`      | `messages`            | `sender_id`     | `users`        |
| `fk_msg_recipient`   | `messages`            | `recipient_id`  | `users`        |
| `fk_msg_lobby`       | `messages`            | `lobby_id`      | `lobbies`      |
| `fk_msg_reply`       | `messages`            | `reply_to_id`   | `messages`     |
| `fk_notif_recipient` | `notifications`       | `recipient_id`  | `users`        |
| `fk_notif_actor`     | `notifications`       | `actor_id`      | `users`        |
| `fk_post_author`     | `posts`               | `author_id`     | `users`        |
| `fk_post_lobby`      | `posts`               | `lobby_id`      | `lobbies`      |
| `fk_media_post`      | `post_media`          | `post_id`       | `posts`        |
| `fk_oauth_user`      | `oauth_accounts`      | `user_id`       | `users`        |

---

## 2.5 Enumeraciones de Dominio

| Enum Java             | Valores                                                                                               |
|-----------------------|-------------------------------------------------------------------------------------------------------|
| `UserStatus`          | `ACTIVE`, `BANNED`, `INACTIVE`                                                                        |
| `LobbyType`           | `COMPETITIVE`, `CASUAL`, `RANKED`                                                                     |
| `LobbyPrivacy`        | `PUBLIC`, `PRIVATE`                                                                                   |
| `MemberRole`          | `OWNER`, `ADMIN`, `MEMBER`                                                                            |
| `JoinRequestStatus`   | `PENDING`, `ACCEPTED`, `REJECTED`                                                                     |
| `MessageType`         | `TEXT`, `IMAGE`, `GIF`, `SYSTEM`                                                                      |
| `MessageStatus`       | `SENT`, `DELIVERED`, `READ`, `DELETED`                                                                |
| `NotificationType`    | `JOIN_REQUEST`, `REQUEST_ACCEPTED`, `REQUEST_REJECTED`, `USER_LEFT`, `USER_JOINED`, `NEW_MESSAGE`, `IMAGE_SENT`, `MENTION`, `POST_APPROVED`, `LOBBY_DELETED`, `SYSTEM` |
| `OAuthProvider`       | `GOOGLE`, `DISCORD`                                                                                   |

> **Nota técnica:** Todos los Enums se mapean como `@Enumerated(EnumType.STRING)` en las entidades JPA, lo que significa que se almacenan como texto legible en MySQL (no como ordinales numéricos), garantizando que la base de datos sea auto-descriptiva.

---

## 2.6 Decisiones de Normalización (3NF)

1. **`lobby_tags`**: Los tags de lobbies se extrajeron de una columna JSON a una tabla independiente `lobby_tags` con restricción `UNIQUE(lobby_id, tag)`. Esto cumple 1NF (eliminando valores multivaluados) y permite indexación individual sobre los tags.

2. **`rank` en `user_games`**: La columna `rank` es una palabra reservada de MySQL 8. Se resolvió escapándola con backticks en la anotación JPA: `@Column(name = "\`rank\`")`.

3. **`messages` dual-purpose**: Un solo modelo soporta tanto chat grupal (cuando `lobby_id != null`) como mensajes directos (cuando `recipient_id != null`), evitando duplicar tablas.
