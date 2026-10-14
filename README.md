[Français](README.md) | [English](README.en.md)

# Square Games API - Plateforme de Jeux de Plateau Multijoueur

API REST et interface web développées avec Spring Boot, proposant un moteur extensible de jeux de plateau au tour par tour (Morpion, Puissance 4, Taquin).

Le projet intègre une architecture modulaire par plugins, la gestion de multiples moteurs de persistance (JPA et JDBC), l'authentification par jeton JWT (clé publique RSA) et l'interopérabilité avec un service externe de gestion d'utilisateurs.

---

## 🎮 Jeux Disponibles

- **Morpion (Tic-Tac-Toe)** : Jeu classique à 2 joueurs sur grille 3x3 ou personnalisée.
- **Puissance 4 (Connect Four)** : Alignement de 4 jetons sur grille 7x6 avec gravité par colonne.
- **Taquin (15-Puzzle)** : Jeu de glissement solitaire pour réordonner les cases numérotées.

---

## 🏗️ Architecture & Fonctionnalités

- **Architecture par Plugins (`GamePlugin`)** : Chaque jeu implémente une interface commune gérant les règles, la validation des coups, l'affichage de la grille et la reconstruction des états de partie.
- **Double Couche de Persistance** :
  - **JPA / Hibernate** (profil `h2`) : Mapping relationnel objet avec entités imbriquées pour les jetons et les positions.
  - **Spring JDBC (`NamedParameterJdbcTemplate`)** (profil `jdbc`) : Requêtes SQL optimisées avec transactions manuelles et mapping explicite.
  - **In-Memory** : Stockage volatile en mémoire pour tests rapides.
- **Sécurité & Authentification JWT** :
  - Filtre `JwtAuthenticationFilter` vérifiant la signature RSA des tokens porteurs.
  - Validation externe des joueurs via client HTTP dédié vers l'API Utilisateurs.
- **Internationalisation (i18n)** : Support dynamique du français et de l'anglais via `Accept-Language`.
- **Interface Web & REST** :
  - API REST complète sous `/games` et `/catalog`.
  - Interface utilisateur web sous `/` pour jouer directement depuis le navigateur.

---

## 📡 Endpoints de l'API REST

| Méthode | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/catalog` | Liste des types de jeux disponibles et configurations acceptées |
| `POST` | `/games` | Création d'une nouvelle partie (`gameType`, `playerCount`, `boardSize`) |
| `GET` | `/games/{id}` | Détail et état actuel du plateau d'une partie |
| `GET` | `/games?player={playerId}` | Liste des parties associées à un identifiant joueur |
| `POST` | `/games/{id}/moves` | Exécution d'un coup (`x`, `y`, `tokenUuid`) |
| `GET` | `/heartbeat` | Sonde de santé et disponibilité de l'application |

---

## 🚀 Démarrage

### Prérequis
- Java 21 ou supérieur
- Apache Maven 3.8+

### Lancer les tests unitaires et d'intégration
```bash
mvn test
```

### Lancer l'application en mode local (JPA + H2)
```bash
mvn spring-boot:run
```

L'application démarre par défaut sur le port `8080` :
- Interface Web : [http://localhost:8080/](http://localhost:8080/)
- API REST : [http://localhost:8080/games](http://localhost:8080/games)
- Catalogue : [http://localhost:8080/catalog](http://localhost:8080/catalog)

### Lancer avec la persistance JDBC
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=jdbc,h2
```

---

## 📜 Licence

Projet sous licence libre à vocation pédagogique et portfolio.
