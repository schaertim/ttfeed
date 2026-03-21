-- Enums
CREATE TYPE game_type AS ENUM ('singles', 'doubles');
CREATE TYPE game_result AS ENUM ('home', 'away', 'not_played');
CREATE TYPE follow_target_type AS ENUM ('player', 'team', 'division');

-- Season, League, Division
CREATE TABLE season (
                        id          SERIAL PRIMARY KEY,
                        name        VARCHAR(9) NOT NULL UNIQUE
);

CREATE TABLE league (
                        id          SERIAL PRIMARY KEY,
                        name        VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE division (
                          id          SERIAL PRIMARY KEY,
                          league_id   INTEGER NOT NULL REFERENCES league(id),
                          season_id   INTEGER NOT NULL REFERENCES season(id),
                          name        VARCHAR(50) NOT NULL,
                          knob_gruppe INTEGER,
                          UNIQUE (league_id, season_id, name)
);

-- Clubs and Teams
CREATE TABLE club (
                      id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      name        VARCHAR(100) NOT NULL,
                      knob_id     INTEGER UNIQUE,
                      clicktt_id  INTEGER UNIQUE
);

CREATE TABLE team (
                      id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      club_id     UUID NOT NULL REFERENCES club(id),
                      division_id INTEGER NOT NULL REFERENCES division(id),
                      name        VARCHAR(100) NOT NULL,
                      knob_id     INTEGER UNIQUE
);

-- Players
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
                               season_id   INTEGER NOT NULL REFERENCES season(id),
                               klass       VARCHAR(5),
                               UNIQUE (player_id, season_id)
);

CREATE TABLE player_elo (
                            id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            player_id   UUID NOT NULL REFERENCES player(id),
                            season_id   INTEGER NOT NULL REFERENCES season(id),
                            elo_value   INTEGER NOT NULL,
                            recorded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Matches
CREATE TABLE match (
                       id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       division_id     INTEGER NOT NULL REFERENCES division(id),
                       home_team_id    UUID NOT NULL REFERENCES team(id),
                       away_team_id    UUID NOT NULL REFERENCES team(id),
                       round           INTEGER,
                       played_at       TIMESTAMPTZ,
                       home_score      SMALLINT,
                       away_score      SMALLINT,
                       knob_match_id   INTEGER UNIQUE,
                       status          VARCHAR(20) NOT NULL DEFAULT 'scheduled',
                       CHECK (home_team_id <> away_team_id)
);

-- Games and Sets
CREATE TABLE game (
                      id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      match_id            UUID NOT NULL REFERENCES match(id),
                      game_type           game_type NOT NULL,
                      order_in_match      SMALLINT NOT NULL,
                      home_player1_id     UUID REFERENCES player(id),
                      home_player2_id     UUID REFERENCES player(id),
                      away_player1_id     UUID REFERENCES player(id),
                      away_player2_id     UUID REFERENCES player(id),
                      home_sets           SMALLINT,
                      away_sets           SMALLINT,
                      result              game_result NOT NULL DEFAULT 'not_played'
);

CREATE TABLE game_set (
                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          game_id         UUID NOT NULL REFERENCES game(id),
                          set_number      SMALLINT NOT NULL,
                          home_points     SMALLINT NOT NULL,
                          away_points     SMALLINT NOT NULL,
                          UNIQUE (game_id, set_number)
);

-- User and Follow system (Phase 3)
CREATE TABLE app_user (
                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          email           VARCHAR(255) UNIQUE NOT NULL,
                          created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE follow (
                        id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        user_id         UUID NOT NULL REFERENCES app_user(id),
                        target_type     follow_target_type NOT NULL,
                        target_id       UUID NOT NULL,
                        created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                        UNIQUE (user_id, target_type, target_id)
);

CREATE TABLE push_subscription (
                                   id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   user_id         UUID NOT NULL REFERENCES app_user(id),
                                   endpoint        TEXT NOT NULL UNIQUE,
                                   p256dh          TEXT NOT NULL,
                                   auth            TEXT NOT NULL,
                                   created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_match_division   ON match(division_id);
CREATE INDEX idx_match_played_at  ON match(played_at);
CREATE INDEX idx_match_status     ON match(status);
CREATE INDEX idx_player_licence   ON player(licence_nr);
CREATE INDEX idx_player_knob      ON player(knob_id);
CREATE INDEX idx_game_match       ON game(match_id);
CREATE INDEX idx_elo_player       ON player_elo(player_id, season_id);
CREATE INDEX idx_follow_user      ON follow(user_id);
CREATE INDEX idx_follow_target    ON follow(target_type, target_id);