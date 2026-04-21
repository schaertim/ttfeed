-- V1: Initial schema

CREATE TABLE season (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name    VARCHAR(9) NOT NULL UNIQUE
);

CREATE TABLE federation (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name    VARCHAR(50) NOT NULL UNIQUE
);

-- Groups sit directly under federation + season (no intermediate division layer).
-- knob_gruppe is nullable because click-tt groups have no knob equivalent.
-- clicktt_id is the `group=` URL param, globally unique within click-tt.
CREATE TABLE division_group (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    federation_id    UUID NOT NULL REFERENCES federation(id),
    season_id        UUID NOT NULL REFERENCES season(id),
    name             VARCHAR(50) NOT NULL,
    knob_gruppe      INTEGER,
    clicktt_id       INTEGER,
    promotion_spots  SMALLINT,
    relegation_spots SMALLINT
);

CREATE UNIQUE INDEX idx_division_group_knob_unique
    ON division_group(federation_id, season_id, knob_gruppe)
    WHERE knob_gruppe IS NOT NULL;

CREATE UNIQUE INDEX idx_division_group_clicktt_unique
    ON division_group(clicktt_id)
    WHERE clicktt_id IS NOT NULL;

CREATE TABLE club (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    clicktt_id  INTEGER UNIQUE
);

-- clicktt_id is the `teamtable=` URL param, globally unique within click-tt.
CREATE TABLE team (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    club_id     UUID NOT NULL REFERENCES club(id),
    group_id    UUID NOT NULL REFERENCES division_group(id),
    name        VARCHAR(100) NOT NULL,
    knob_id     INTEGER,
    clicktt_id  INTEGER
);

CREATE UNIQUE INDEX idx_team_clicktt_unique
    ON team(clicktt_id)
    WHERE clicktt_id IS NOT NULL;

CREATE TABLE standing (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id      UUID NOT NULL REFERENCES division_group(id),
    team_id       UUID NOT NULL REFERENCES team(id),
    position      SMALLINT NOT NULL,
    played        SMALLINT NOT NULL,
    won           SMALLINT NOT NULL,
    drawn         SMALLINT NOT NULL,
    lost          SMALLINT NOT NULL,
    games_for     SMALLINT NOT NULL,
    games_against SMALLINT NOT NULL,
    points        SMALLINT NOT NULL,
    UNIQUE (group_id, team_id)
);

CREATE TABLE player (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    licence_nr  VARCHAR(20) UNIQUE NOT NULL,
    knob_id     INTEGER UNIQUE,
    clicktt_id  INTEGER UNIQUE,
    full_name   VARCHAR(100) NOT NULL
);

CREATE TABLE player_season (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id   UUID NOT NULL REFERENCES player(id),
    team_id     UUID NOT NULL REFERENCES team(id),
    season_id   UUID NOT NULL REFERENCES season(id),
    klass       VARCHAR(5),
    UNIQUE (player_id, season_id)
);

CREATE TABLE player_elo (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id   UUID NOT NULL REFERENCES player(id),
    season_id   UUID NOT NULL REFERENCES season(id),
    elo_value   INTEGER NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- clicktt_match_id is the `meeting=` URL param, globally unique within click-tt.
CREATE TABLE match (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id          UUID NOT NULL REFERENCES division_group(id),
    home_team_id      UUID NOT NULL REFERENCES team(id),
    away_team_id      UUID NOT NULL REFERENCES team(id),
    round             VARCHAR(50),
    played_at         TIMESTAMPTZ,
    home_score        SMALLINT,
    away_score        SMALLINT,
    knob_match_id     INTEGER,
    clicktt_match_id  INTEGER,
    status            VARCHAR(20) NOT NULL DEFAULT 'scheduled',
    CHECK (home_team_id <> away_team_id)
);

CREATE UNIQUE INDEX idx_match_clicktt_unique
    ON match(clicktt_match_id)
    WHERE clicktt_match_id IS NOT NULL;

CREATE TABLE game (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    match_id                 UUID REFERENCES match(id),
    game_type                VARCHAR(10) NOT NULL,
    order_in_match           SMALLINT,
    home_player1_id          UUID REFERENCES player(id),
    home_player2_id          UUID REFERENCES player(id),
    away_player1_id          UUID REFERENCES player(id),
    away_player2_id          UUID REFERENCES player(id),
    home_sets                SMALLINT,
    away_sets                SMALLINT,
    result                   VARCHAR(10) NOT NULL DEFAULT 'NOT_PLAYED',
    home_player1_elo_delta   NUMERIC(5,3),
    away_player1_elo_delta   NUMERIC(5,3),
    competition_name         VARCHAR(255),
    played_at                TIMESTAMPTZ,
    CONSTRAINT uq_game_match_order UNIQUE (match_id, order_in_match)
);

CREATE TABLE game_set (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id     UUID NOT NULL REFERENCES game(id),
    set_number  SMALLINT NOT NULL,
    home_points SMALLINT NOT NULL,
    away_points SMALLINT NOT NULL,
    UNIQUE (game_id, set_number)
);

-- Phase 3 tables (defined now, implemented later)
CREATE TABLE app_user (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) UNIQUE NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TYPE follow_target_type AS ENUM ('player', 'team', 'division_group');

CREATE TABLE follow (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_user(id),
    target_type follow_target_type NOT NULL,
    target_id   UUID NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, target_type, target_id)
);

CREATE TABLE push_subscription (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_user(id),
    endpoint    TEXT NOT NULL UNIQUE,
    p256dh      TEXT NOT NULL,
    auth        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_division_group_federation_season ON division_group(federation_id, season_id);
CREATE INDEX idx_team_group                        ON team(group_id);
CREATE INDEX idx_team_knob_group                   ON team(knob_id, group_id);
CREATE INDEX idx_standing_group                    ON standing(group_id);
CREATE INDEX idx_match_group                       ON match(group_id);
CREATE INDEX idx_match_knob_group                  ON match(knob_match_id, group_id);
CREATE INDEX idx_match_played_at                   ON match(played_at);
CREATE INDEX idx_match_status                      ON match(status);
CREATE INDEX idx_player_licence                    ON player(licence_nr);
CREATE INDEX idx_player_knob                       ON player(knob_id);
CREATE INDEX idx_game_match                        ON game(match_id);
CREATE INDEX idx_elo_player                        ON player_elo(player_id, season_id);
CREATE INDEX idx_follow_user                       ON follow(user_id);
CREATE INDEX idx_follow_target                     ON follow(target_type, target_id);
