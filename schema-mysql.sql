-- ============================================================
--  SquadUp — Script MySQL
--  Equivalente al esquema PostgreSQL, adaptado a MySQL 8+
-- ============================================================

CREATE DATABASE IF NOT EXISTS squadup_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE squadup_db;

-- ────────────────────────────────────────────
--  TABLAS
-- ────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)     NOT NULL,
    full_name     VARCHAR(120)    NOT NULL,
    email         VARCHAR(255)    NOT NULL,
    password_hash VARCHAR(255)    NULL,
    avatar_url    TEXT            NULL,
    status        ENUM('active','banned','inactive') NOT NULL DEFAULT 'active',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login    DATETIME        NULL,

    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_email    CHECK (email LIKE '%@%')
);

CREATE TABLE IF NOT EXISTS oauth_accounts (
    id               BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT          NOT NULL,
    provider         ENUM('google','discord') NOT NULL,
    provider_uid     VARCHAR(255)    NOT NULL,
    access_token     TEXT            NULL,
    refresh_token    TEXT            NULL,
    token_expires_at DATETIME        NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_oauth_provider_uid UNIQUE (provider, provider_uid),
    CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS games (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    genre       ENUM('fps','moba','rpg','battle_royale','strategy','sports','survival','sandbox','other') NOT NULL DEFAULT 'other',
    cover_url   TEXT        NULL,
    description TEXT        NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_games_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS user_games (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT      NOT NULL,
    game_id      BIGINT      NOT NULL,
    `rank`       ENUM('unranked','bronze','silver','gold','platinum','diamond','master','challenger') NOT NULL DEFAULT 'unranked',
    rank_label   VARCHAR(50) NULL,
    hours_played INT         NULL CHECK (hours_played >= 0),
    is_main      TINYINT(1)  NOT NULL DEFAULT 0,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_user_games UNIQUE (user_id, game_id),
    CONSTRAINT fk_ug_user   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ug_game   FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS lobbies (
    id          BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    owner_id    BIGINT          NOT NULL,
    game_id     BIGINT          NULL,
    name        VARCHAR(100)    NOT NULL,
    description TEXT            NULL,
    image_url   TEXT            NULL,
    lobby_type  ENUM('competitive','casual','ranked') NOT NULL DEFAULT 'casual',
    privacy     ENUM('public','private')              NOT NULL DEFAULT 'public',
    max_members SMALLINT        NOT NULL DEFAULT 10,
    tags        JSON            NULL,
    extra_meta  JSON            NULL,
    is_active   TINYINT(1)      NOT NULL DEFAULT 1,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT ck_lobby_max_members CHECK (max_members BETWEEN 2 AND 500),
    CONSTRAINT fk_lobby_owner FOREIGN KEY (owner_id) REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_lobby_game  FOREIGN KEY (game_id)  REFERENCES games(id)  ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS lobby_members (
    id        BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lobby_id  BIGINT      NOT NULL,
    user_id   BIGINT      NOT NULL,
    role      ENUM('owner','admin','member') NOT NULL DEFAULT 'member',
    joined_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_lobby_members UNIQUE (lobby_id, user_id),
    CONSTRAINT fk_lm_lobby FOREIGN KEY (lobby_id) REFERENCES lobbies(id) ON DELETE CASCADE,
    CONSTRAINT fk_lm_user  FOREIGN KEY (user_id)  REFERENCES users(id)   ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS lobby_join_requests (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lobby_id     BIGINT      NOT NULL,
    requester_id BIGINT      NOT NULL,
    reviewed_by  BIGINT      NULL,
    status       ENUM('pending','accepted','rejected') NOT NULL DEFAULT 'pending',
    message      TEXT        NULL,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at  DATETIME    NULL,

    CONSTRAINT fk_jr_lobby    FOREIGN KEY (lobby_id)     REFERENCES lobbies(id) ON DELETE CASCADE,
    CONSTRAINT fk_jr_requester FOREIGN KEY (requester_id) REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_jr_reviewer  FOREIGN KEY (reviewed_by)  REFERENCES users(id)  ON DELETE SET NULL
);

-- La unicidad de solicitudes PENDING se controla en LobbyService.requestJoin().
-- Un índice UNIQUE aquí bloquearía re-solicitudes tras un rechazo, por eso se usa índice simple.
CREATE INDEX ix_join_req_lobby_user
    ON lobby_join_requests (lobby_id, requester_id);

CREATE TABLE IF NOT EXISTS posts (
    id                BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lobby_id          BIGINT      NOT NULL,
    author_id         BIGINT      NOT NULL,
    content           TEXT        NULL,
    is_pinned         TINYINT(1)  NOT NULL DEFAULT 0,
    requires_approval TINYINT(1)  NOT NULL DEFAULT 0,
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        DATETIME    NULL,

    CONSTRAINT fk_post_lobby  FOREIGN KEY (lobby_id)  REFERENCES lobbies(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_author FOREIGN KEY (author_id) REFERENCES users(id)   ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS post_media (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT      NOT NULL,
    media_type ENUM('image','gif','video','link') NOT NULL DEFAULT 'image',
    url        TEXT        NOT NULL,
    filename   VARCHAR(255) NULL,
    file_size  BIGINT      NULL CHECK (file_size > 0),
    meta       JSON        NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pm_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS saved_posts (
    id       BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id  BIGINT   NOT NULL,
    post_id  BIGINT   NOT NULL,
    saved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_saved_posts UNIQUE (user_id, post_id),
    CONSTRAINT fk_sp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sp_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS messages (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lobby_id     BIGINT      NULL,
    sender_id    BIGINT      NULL,
    recipient_id BIGINT      NULL,
    reply_to_id  BIGINT      NULL,
    content      TEXT        NULL,
    msg_type     ENUM('text','image','gif','system')              NOT NULL DEFAULT 'text',
    status       ENUM('sent','delivered','read','deleted')        NOT NULL DEFAULT 'sent',
    attachment   JSON        NULL,
    sent_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edited_at    DATETIME    NULL,
    deleted_at   DATETIME    NULL,

    CONSTRAINT fk_msg_lobby     FOREIGN KEY (lobby_id)     REFERENCES lobbies(id)  ON DELETE CASCADE,
    CONSTRAINT fk_msg_sender    FOREIGN KEY (sender_id)    REFERENCES users(id)    ON DELETE SET NULL,
    CONSTRAINT fk_msg_recipient FOREIGN KEY (recipient_id) REFERENCES users(id)    ON DELETE SET NULL,
    CONSTRAINT fk_msg_reply     FOREIGN KEY (reply_to_id)  REFERENCES messages(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT      NOT NULL,
    actor_id     BIGINT      NULL,
    notif_type   ENUM(
        'join_request','request_accepted','request_rejected',
        'user_left','user_joined','new_message','image_sent',
        'mention','post_approved','lobby_deleted','system'
    ) NOT NULL,
    payload      JSON        NULL,
    is_read      TINYINT(1)  NOT NULL DEFAULT 0,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at      DATETIME    NULL,

    CONSTRAINT fk_notif_recipient FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_actor     FOREIGN KEY (actor_id)     REFERENCES users(id) ON DELETE SET NULL
);

-- ────────────────────────────────────────────
--  ÍNDICES
-- ────────────────────────────────────────────
CREATE INDEX ix_users_email    ON users (email);
CREATE INDEX ix_users_username ON users (username);
CREATE INDEX ix_users_status   ON users (status);
CREATE INDEX ix_oauth_user_id  ON oauth_accounts (user_id);
CREATE INDEX ix_games_name     ON games (name);
CREATE INDEX ix_games_genre    ON games (genre);
CREATE INDEX ix_user_games_user ON user_games (user_id);
CREATE INDEX ix_user_games_game ON user_games (game_id);
CREATE INDEX ix_lobbies_owner   ON lobbies (owner_id);
CREATE INDEX ix_lobbies_game    ON lobbies (game_id);
CREATE INDEX ix_lobbies_privacy ON lobbies (privacy);
CREATE INDEX ix_lobbies_active  ON lobbies (is_active);
CREATE INDEX ix_lobby_members_lobby ON lobby_members (lobby_id);
CREATE INDEX ix_lobby_members_user  ON lobby_members (user_id);
CREATE INDEX ix_join_req_lobby  ON lobby_join_requests (lobby_id);
CREATE INDEX ix_join_req_user   ON lobby_join_requests (requester_id);
CREATE INDEX ix_join_req_status ON lobby_join_requests (status);
CREATE INDEX ix_posts_lobby     ON posts (lobby_id, created_at);
CREATE INDEX ix_posts_author    ON posts (author_id);
CREATE INDEX ix_post_media_post ON post_media (post_id, sort_order);
CREATE INDEX ix_saved_posts_user ON saved_posts (user_id, saved_at);
CREATE INDEX ix_messages_lobby   ON messages (lobby_id, sent_at);
CREATE INDEX ix_messages_sender  ON messages (sender_id);
CREATE INDEX ix_messages_dm      ON messages (sender_id, recipient_id, sent_at);
CREATE INDEX ix_notif_recipient  ON notifications (recipient_id, created_at);
CREATE INDEX ix_notif_type       ON notifications (notif_type);

-- ────────────────────────────────────────────
--  DATOS INICIALES (seed)
-- ────────────────────────────────────────────
INSERT INTO games (name, genre, description) VALUES
    ('Doom Eternal',          'fps',          'shooter infernal de id Software'),
    ('League of Legends',     'moba',         'batalla 5v5 en la grieta del invocador'),
    ('ARK: Survival Evolved', 'survival',     'supervivencia con dinosaurios'),
    ('Fortnite',              'battle_royale','battle royale de Epic Games'),
    ('Minecraft',             'sandbox',      'mundo de bloques infinito'),
    ('Valorant',              'fps',          'shooter táctico de Riot Games');
