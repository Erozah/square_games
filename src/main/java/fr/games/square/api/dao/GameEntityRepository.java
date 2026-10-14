package fr.games.square.api.dao;

import fr.games.square.api.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameEntityRepository extends JpaRepository<GameEntity, String> {

    @Query("SELECT DISTINCT g FROM GameEntity g JOIN g.playerIds p WHERE p = :userId")
    List<GameEntity> findByPlayerId(@Param("userId") UUID userId);

    default List<GameEntity> findByPlayerIdContaining(String userId) {
        return findByPlayerId(UUID.fromString(userId));
    }
}
