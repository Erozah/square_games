[Français](README.md) | [English](README.en.md)

# Square Games API - Multiplayer Board Game Platform

REST API and web UI built with Spring Boot, delivering an extensible turn-based board game engine (Tic-Tac-Toe, Connect Four, 15-Puzzle).

The project features a modular plugin architecture, dual persistence engines (JPA and Spring JDBC), JWT authentication with RSA public-key validation, and external player identity verification.

---

## 🎮 Available Games

- **Tic-Tac-Toe**: Classic 2-player game on 3x3 or custom grid dimensions.
- **Connect Four**: 4-in-a-row token alignment on a 7x6 board with column-based gravity physics.
- **15-Puzzle (Taquin)**: Single-player sliding tile puzzle to order numbered tokens.

---

## 🏗️ Architecture & Highlights

- **Plugin Architecture (`GamePlugin`)**: Each game type is self-contained and implements standard hooks for game rules, move validation, grid rendering, and board state restoration.
- **Dual Persistence Layer**:
  - **JPA / Hibernate** (`h2` profile): Object-relational mapping with child entities for tokens and coordinates.
  - **Spring JDBC (`NamedParameterJdbcTemplate`)** (`jdbc` profile): Handcrafted SQL queries with custom row mappers and transactional integrity.
  - **In-Memory**: Volatile storage mode for isolated testing.
- **Security & JWT Authentication**:
  - `JwtAuthenticationFilter` verifying RSA public-key signatures on incoming bearer tokens.
  - External player existence check against the dedicated Users microservice.
- **Internationalization (i18n)**: Locale-aware error messages and game labels in English and French.
- **Web UI & REST Interfaces**:
  - Clean REST API exposed under `/games` and `/catalog`.
  - Server-side rendered web view at `/` to play games directly in the browser.

---

## 📡 REST API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/catalog` | List available game types, rules, and allowed dimensions |
| `POST` | `/games` | Create a new game instance (`gameType`, `playerCount`, `boardSize`) |
| `GET` | `/games/{id}` | Retrieve current board state, active player, and moves |
| `GET` | `/games?player={playerId}` | Find all games associated with a given player ID |
| `POST` | `/games/{id}/moves` | Submit a move (`x`, `y`, `tokenUuid`) |
| `GET` | `/heartbeat` | Health check and application status endpoint |

---

## 🚀 Getting Started

### Prerequisites
- Java 21 or higher
- Apache Maven 3.8+

### Run Unit and Integration Tests
```bash
mvn test
```

### Run Locally (JPA + H2 default)
```bash
mvn spring-boot:run
```

The application runs on port `8080` by default:
- Web UI: [http://localhost:8080/](http://localhost:8080/)
- REST API: [http://localhost:8080/games](http://localhost:8080/games)
- Catalog: [http://localhost:8080/catalog](http://localhost:8080/catalog)

### Run with Spring JDBC Persistence
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=jdbc,h2
```

---

## 📜 License

Open-source project built for educational and portfolio demonstration purposes.
