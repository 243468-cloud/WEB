-- =====================================================
-- schema-h2.sql — SquadUp Dev (H2 compatible)
-- Sustituye: TIMESTAMPTZ→TIMESTAMP, jsonb→CLOB, TEXT[]→VARCHAR
-- =====================================================

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    full_name   VARCHAR(120),
    password_hash TEXT,
    avatar_url  TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS oauth_accounts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider        VARCHAR(20)  NOT NULL,
    provider_uid    VARCHAR(255) NOT NULL,
    access_token    TEXT,
    refresh_token   TEXT,
    token_expires_at TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(provider, provider_uid)
);

CREATE TABLE IF NOT EXISTS games (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL UNIQUE,
    genre       VARCHAR(30)  NOT NULL,
    cover_url   TEXT,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_games (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    game_id      BIGINT      NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    rank         VARCHAR(20),
    rank_label   VARCHAR(50),
    hours_played INT,
    is_main      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, game_id)
);

CREATE TABLE IF NOT EXISTS lobbies (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id    BIGINT       NOT NULL REFERENCES users(id),
    game_id     BIGINT       REFERENCES games(id),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    image_url   TEXT,
    lobby_type  VARCHAR(20)  NOT NULL DEFAULT 'CASUAL',
    privacy     VARCHAR(10)  NOT NULL DEFAULT 'PUBLIC',
    max_members SMALLINT     NOT NULL DEFAULT 10,
    tags        VARCHAR(2000),          -- TEXT[] simplificado
    extra_meta  CLOB,                   -- jsonb simplificado
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lobby_members (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    lobby_id  BIGINT      NOT NULL REFERENCES lobbies(id) ON DELETE CASCADE,
    user_id   BIGINT      NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    role      VARCHAR(10) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(lobby_id, user_id)
);

CREATE TABLE IF NOT EXISTS lobby_join_requests (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    lobby_id     BIGINT      NOT NULL REFERENCES lobbies(id) ON DELETE CASCADE,
    requester_id BIGINT      NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    reviewed_by  BIGINT      REFERENCES users(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message      TEXT,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS posts (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    lobby_id         BIGINT    NOT NULL REFERENCES lobbies(id) ON DELETE CASCADE,
    author_id        BIGINT    NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
    content          TEXT,
    is_pinned        BOOLEAN   NOT NULL DEFAULT FALSE,
    requires_approval BOOLEAN  NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP
);

CREATE TABLE IF NOT EXISTS post_media (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    media_type VARCHAR(10) NOT NULL,
    url        TEXT        NOT NULL,
    filename   VARCHAR(255),
    file_size  BIGINT,
    sort_order INT         NOT NULL DEFAULT 0,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS saved_posts (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id  BIGINT    NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    post_id  BIGINT    NOT NULL REFERENCES posts(id)  ON DELETE CASCADE,
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, post_id)
);

CREATE TABLE IF NOT EXISTS messages (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    lobby_id     BIGINT      REFERENCES lobbies(id)  ON DELETE CASCADE,
    sender_id    BIGINT      REFERENCES users(id)    ON DELETE SET NULL,
    recipient_id BIGINT      REFERENCES users(id)    ON DELETE SET NULL,
    reply_to_id  BIGINT      REFERENCES messages(id),
    content      TEXT,
    msg_type     VARCHAR(10) NOT NULL DEFAULT 'TEXT',
    status       VARCHAR(10) NOT NULL DEFAULT 'SENT',
    attachment   CLOB,                               -- jsonb simplificado
    sent_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edited_at    TIMESTAMP,
    deleted_at   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notifications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    actor_id     BIGINT      REFERENCES users(id) ON DELETE SET NULL,
    notif_type   VARCHAR(30) NOT NULL,
    payload      CLOB,                               -- jsonb simplificado
    is_read      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at      TIMESTAMP
);
