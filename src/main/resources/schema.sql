CREATE TABLE IF NOT EXISTS games (
    id VARCHAR(255) PRIMARY KEY,
    factory_id VARCHAR(255),
    board_size INT,
    version BIGINT DEFAULT 0
);

ALTER TABLE games ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

CREATE TABLE IF NOT EXISTS game_players (
    game_id VARCHAR(255) NOT NULL,
    player_id UUID NOT NULL,
    PRIMARY KEY (game_id, player_id),
    CONSTRAINT fk_game_players_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_game_players_player_id ON game_players(player_id);

CREATE TABLE IF NOT EXISTS game_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id VARCHAR(255),
    owner_id VARCHAR(255),
    name VARCHAR(255),
    removed BOOLEAN,
    x INT,
    y INT
);
