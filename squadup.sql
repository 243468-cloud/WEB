-- ============================================================
--  SquadUp — Script PostgreSQL
--  Base de datos normalizada (3FN) para la app LFG




-- ────────────────────────────────────────────
--  BASE DE DATOS
-- ────────────────────────────────────────────
CREATE DATABASE squadup_db
    WITH ENCODING = 'UTF8'
         LC_COLLATE = 'es_MX.UTF-8'
         LC_CTYPE   = 'es_MX.UTF-8'
         TEMPLATE   = template0;

\connect squadup_db



CREATE EXTENSION IF NOT EXISTS "pgcrypto";   
CREATE EXTENSION IF NOT EXISTS "pg_trgm";    



CREATE TYPE user_status_t       AS ENUM ('active', 'banned', 'inactive');
CREATE TYPE oauth_provider_t    AS ENUM ('google', 'discord');
CREATE TYPE game_genre_t        AS ENUM ('fps','moba','rpg','battle_royale',
                                          'strategy','sports','survival','sandbox','other');
CREATE TYPE player_rank_t       AS ENUM ('unranked','bronze','silver','gold',
                                          'platinum','diamond','master','challenger');
CREATE TYPE lobby_type_t        AS ENUM ('competitive','casual','ranked');
CREATE TYPE lobby_privacy_t     AS ENUM ('public','private');
CREATE TYPE member_role_t       AS ENUM ('owner','admin','member');
CREATE TYPE join_req_status_t   AS ENUM ('pending','accepted','rejected');
CREATE TYPE media_type_t        AS ENUM ('image','gif','video','link');
CREATE TYPE message_type_t      AS ENUM ('text','image','gif','system');
CREATE TYPE message_status_t    AS ENUM ('sent','delivered','read','deleted');
CREATE TYPE notification_type_t AS ENUM (
    'join_request','request_accepted','request_rejected',
    'user_left','user_joined','new_message','image_sent',
    'mention','post_approved','lobby_deleted','system'
);


CREATE TABLE users (
    id            BIGSERIAL       PRIMARY KEY,
    username      VARCHAR(50)     NOT NULL,
    full_name     VARCHAR(120)    NOT NULL,
    email         VARCHAR(255)    NOT NULL,
    password_hash VARCHAR(255)    NULL,        
    avatar_url    TEXT            NULL,
    status        user_status_t   NOT NULL DEFAULT 'active',
    created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    last_login    TIMESTAMPTZ     NULL,

    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_email    CHECK (email LIKE '%@%')
);

COMMENT ON TABLE  users               IS 'Jugadores registrados en SquadUp';
COMMENT ON COLUMN users.password_hash IS 'Hash bcrypt; NULL cuando la cuenta es sólo OAuth2';
COMMENT ON COLUMN users.status        IS 'active | banned | inactive';

CREATE INDEX ix_users_email    ON users (email);
CREATE INDEX ix_users_username ON users (username);
CREATE INDEX ix_users_status   ON users (status);
-- Búsqueda difusa por username (pg_trgm)
CREATE INDEX ix_users_username_trgm ON users USING gin (username gin_trgm_ops);


CREATE TABLE oauth_accounts (
    id               BIGSERIAL        PRIMARY KEY,
    user_id          BIGINT           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider         oauth_provider_t NOT NULL,
    provider_uid     VARCHAR(255)     NOT NULL,   
    access_token     TEXT             NULL,        
    refresh_token    TEXT             NULL,
    token_expires_at TIMESTAMPTZ      NULL,
    created_at       TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_oauth_provider_uid UNIQUE (provider, provider_uid)
);

COMMENT ON TABLE oauth_accounts IS 'Proveedores OAuth2 vinculados a cada usuario (Google, Discord)';

CREATE INDEX ix_oauth_user_id ON oauth_accounts (user_id);


CREATE TABLE games (
    id          BIGSERIAL      PRIMARY KEY,
    name        VARCHAR(120)   NOT NULL,
    genre       game_genre_t   NOT NULL DEFAULT 'other',
    cover_url   TEXT           NULL,
    description TEXT           NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_games_name UNIQUE (name)
);

COMMENT ON TABLE games IS 'Catálogo maestro de videojuegos disponibles en la plataforma';

CREATE INDEX ix_games_name  ON games (name);
CREATE INDEX ix_games_genre ON games (genre);
CREATE INDEX ix_games_name_trgm ON games USING gin (name gin_trgm_ops);


CREATE TABLE user_games (
    id           BIGSERIAL      PRIMARY KEY,
    user_id      BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    game_id      BIGINT         NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    rank         player_rank_t  NOT NULL DEFAULT 'unranked',
    rank_label   VARCHAR(50)    NULL,     -- Rango exacto del juego: "Diamante I"
    hours_played INT            NULL CHECK (hours_played >= 0),
    is_main      BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_games UNIQUE (user_id, game_id)
);

COMMENT ON TABLE  user_games          IS 'Juegos que un jugador juega y su rango';
COMMENT ON COLUMN user_games.is_main  IS 'Marca el juego principal del usuario en su perfil';

CREATE INDEX ix_user_games_user ON user_games (user_id);
CREATE INDEX ix_user_games_game ON user_games (game_id);


CREATE TABLE lobbies (
    id          BIGSERIAL       PRIMARY KEY,
    owner_id    BIGINT          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    game_id     BIGINT          NULL     REFERENCES games (id) ON DELETE SET NULL,
    name        VARCHAR(100)    NOT NULL,
    description TEXT            NULL,
    image_url   TEXT            NULL,
    lobby_type  lobby_type_t    NOT NULL DEFAULT 'casual',
    privacy     lobby_privacy_t NOT NULL DEFAULT 'public',
    max_members SMALLINT        NOT NULL DEFAULT 10,
    tags        TEXT[]          NULL,     -- Array de etiquetas (PostgreSQL nativo)
    extra_meta  JSONB           NULL,     -- Metadatos extendidos: modo de juego, rango mínimo…
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_lobby_max_members CHECK (max_members BETWEEN 2 AND 500)
);

COMMENT ON TABLE  lobbies            IS 'Salas de juego creadas por los jugadores';
COMMENT ON COLUMN lobbies.tags       IS 'Etiquetas libres; indexadas con GIN para búsqueda rápida';
COMMENT ON COLUMN lobbies.extra_meta IS 'JSONB flexible: {"min_rank":"gold","region":"LATAM"}';

CREATE INDEX ix_lobbies_owner   ON lobbies (owner_id);
CREATE INDEX ix_lobbies_game    ON lobbies (game_id);
CREATE INDEX ix_lobbies_privacy ON lobbies (privacy);
CREATE INDEX ix_lobbies_active  ON lobbies (is_active);
-- GIN sobre array de tags
CREATE INDEX ix_lobbies_tags_gin ON lobbies USING gin (tags);
-- GIN sobre JSONB para consultas por campo específico
CREATE INDEX ix_lobbies_meta_gin ON lobbies USING gin (extra_meta);
-- Búsqueda por nombre
CREATE INDEX ix_lobbies_name_trgm ON lobbies USING gin (name gin_trgm_ops);


CREATE TABLE lobby_members (
    id        BIGSERIAL     PRIMARY KEY,
    lobby_id  BIGINT        NOT NULL REFERENCES lobbies (id) ON DELETE CASCADE,
    user_id   BIGINT        NOT NULL REFERENCES users   (id) ON DELETE CASCADE,
    role      member_role_t NOT NULL DEFAULT 'member',
    joined_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_lobby_members UNIQUE (lobby_id, user_id)
);

COMMENT ON TABLE lobby_members IS 'Miembros aceptados en un lobby';

CREATE INDEX ix_lobby_members_lobby ON lobby_members (lobby_id);
CREATE INDEX ix_lobby_members_user  ON lobby_members (user_id);


CREATE TABLE lobby_join_requests (
    id           BIGSERIAL           PRIMARY KEY,
    lobby_id     BIGINT              NOT NULL REFERENCES lobbies (id) ON DELETE CASCADE,
    requester_id BIGINT              NOT NULL REFERENCES users   (id) ON DELETE CASCADE,
    reviewed_by  BIGINT              NULL     REFERENCES users   (id) ON DELETE SET NULL,
    status       join_req_status_t   NOT NULL DEFAULT 'pending',
    message      TEXT                NULL,
    created_at   TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    reviewed_at  TIMESTAMPTZ         NULL
);

COMMENT ON TABLE lobby_join_requests IS 'Solicitudes de ingreso a lobbies privados';

CREATE INDEX ix_join_req_lobby  ON lobby_join_requests (lobby_id);
CREATE INDEX ix_join_req_user   ON lobby_join_requests (requester_id);
CREATE INDEX ix_join_req_status ON lobby_join_requests (status);
-- Índice parcial: impide solicitudes duplicadas pendientes al mismo lobby
CREATE UNIQUE INDEX ix_join_req_pending_uniq
    ON lobby_join_requests (lobby_id, requester_id)
    WHERE status = 'pending';


CREATE TABLE posts (
    id               BIGSERIAL   PRIMARY KEY,
    lobby_id         BIGINT      NOT NULL REFERENCES lobbies (id) ON DELETE CASCADE,
    author_id        BIGINT      NOT NULL REFERENCES users   (id) ON DELETE CASCADE,
    content          TEXT        NULL,
    is_pinned        BOOLEAN     NOT NULL DEFAULT FALSE,
    requires_approval BOOLEAN    NOT NULL DEFAULT FALSE, -- Control de acceso al archivo
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ NULL      -- Soft-delete
);

COMMENT ON TABLE  posts                   IS 'Publicaciones persistentes dentro de un lobby';
COMMENT ON COLUMN posts.requires_approval IS 'Si TRUE, otros usuarios necesitan aprobación para ver el archivo adjunto';
COMMENT ON COLUMN posts.deleted_at        IS 'Soft-delete: NULL = activo';

CREATE INDEX ix_posts_lobby      ON posts (lobby_id, created_at);
CREATE INDEX ix_posts_author     ON posts (author_id);
-- Índice parcial — sólo posts activos (los que más se consultan)
CREATE INDEX ix_posts_active
    ON posts (lobby_id, created_at)
    WHERE deleted_at IS NULL;


CREATE TABLE post_media (
    id         BIGSERIAL    PRIMARY KEY,
    post_id    BIGINT       NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    media_type media_type_t NOT NULL DEFAULT 'image',
    url        TEXT         NOT NULL,
    filename   VARCHAR(255) NULL,
    file_size  BIGINT       NULL CHECK (file_size > 0),  -- Bytes
    meta       JSONB        NULL,  -- {width, height, duration, mime_type}
    sort_order INT          NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE post_media IS 'Archivos multimedia adjuntos a una publicación (1FN: múltiples archivos por post)';

CREATE INDEX ix_post_media_post ON post_media (post_id, sort_order);


CREATE TABLE saved_posts (
    id       BIGSERIAL   PRIMARY KEY,
    user_id  BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    post_id  BIGINT      NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    saved_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_saved_posts UNIQUE (user_id, post_id)
);

COMMENT ON TABLE saved_posts IS 'Publicaciones guardadas/marcadas por el usuario (bookmarks)';

CREATE INDEX ix_saved_posts_user ON saved_posts (user_id, saved_at DESC);



CREATE TABLE messages (
    id           BIGSERIAL        PRIMARY KEY,
    lobby_id     BIGINT           NULL REFERENCES lobbies  (id) ON DELETE CASCADE,
    sender_id    BIGINT           NULL REFERENCES users    (id) ON DELETE SET NULL,
    recipient_id BIGINT           NULL REFERENCES users    (id) ON DELETE SET NULL,
    reply_to_id  BIGINT           NULL REFERENCES messages (id) ON DELETE SET NULL,
    content      TEXT             NULL,
    msg_type     message_type_t   NOT NULL DEFAULT 'text',
    status       message_status_t NOT NULL DEFAULT 'sent',
    attachment   JSONB            NULL,   -- {url, filename, size, width, height}
    sent_at      TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    edited_at    TIMESTAMPTZ      NULL,
    deleted_at   TIMESTAMPTZ      NULL,   -- Soft-delete

    -- Un mensaje pertenece a un lobby O es DM, no ambos ni ninguno
    CONSTRAINT ck_message_target CHECK (
        (lobby_id IS NOT NULL AND recipient_id IS NULL) OR
        (lobby_id IS NULL     AND recipient_id IS NOT NULL)
    )
);

COMMENT ON TABLE  messages              IS 'Mensajes en tiempo real: chat de lobby y DM privado 1:1';
COMMENT ON COLUMN messages.recipient_id IS 'Solo para DM privado; NULL en chat de lobby';
COMMENT ON COLUMN messages.reply_to_id  IS 'Autorreferencia para hilos de respuesta';

CREATE INDEX ix_messages_lobby  ON messages (lobby_id, sent_at);
CREATE INDEX ix_messages_sender ON messages (sender_id);
CREATE INDEX ix_messages_dm     ON messages (sender_id, recipient_id, sent_at);
-- Índice parcial — mensajes activos (no eliminados)
CREATE INDEX ix_messages_active
    ON messages (lobby_id, sent_at)
    WHERE deleted_at IS NULL;




CREATE TABLE notifications (
    id           BIGSERIAL            PRIMARY KEY,
    recipient_id BIGINT               NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    actor_id     BIGINT               NULL     REFERENCES users (id) ON DELETE SET NULL,
    notif_type   notification_type_t  NOT NULL,
    payload      JSONB                NULL,   -- Contexto según tipo (lobby_id, post_id…)
    is_read      BOOLEAN              NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ          NOT NULL DEFAULT NOW(),
    read_at      TIMESTAMPTZ          NULL
);

COMMENT ON TABLE  notifications         IS 'Alertas en tiempo real entregadas por WebSocket';
COMMENT ON COLUMN notifications.payload IS
    'JSONB flexible según notif_type: join_request → {lobby_id, lobby_name, requester_id}; mention → {post_id, lobby_id}';

CREATE INDEX ix_notif_recipient ON notifications (recipient_id, created_at DESC);

CREATE UNIQUE INDEX ix_notif_unread
    ON notifications (recipient_id, id)
    WHERE is_read = FALSE;
CREATE INDEX ix_notif_type ON notifications (notif_type);

CREATE INDEX ix_notif_payload_gin ON notifications USING gin (payload);



CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_lobbies_updated_at
    BEFORE UPDATE ON lobbies
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_posts_updated_at
    BEFORE UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();


-- ============================================================
--  DATOS INICIALES (seed)
-- ============================================================

INSERT INTO games (name, genre, description) VALUES
    ('Doom Eternal',          'fps',           'shooter infernal de id Software'),
    ('League of Legends',     'moba',          'batalla 5v5 en la grieta del invocador'),
    ('ARK: Survival Evolved', 'survival',      'supervivencia con dinosaurios'),
    ('Fortnite',              'battle_royale',  'battle royale de Epic Games'),
    ('Minecraft',             'sandbox',        'mundo de bloques infinito'),
    ('Valorant',              'fps',            'shooter táctico de Riot Games');

-- ============================================================
--  FIN DEL SCRIPT
-- ============================================================
