CREATE TABLE IF NOT EXISTS games (
    id VARCHAR(36) PRIMARY KEY,
    factory_id VARCHAR(255) NOT NULL,
    board_size INT NOT NULL,
    player_ids VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS game_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id VARCHAR(36),
    owner_id VARCHAR(36),
    name VARCHAR(255) NOT NULL,
    removed BOOLEAN NOT NULL,
    x INT,
    y INT,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
);
