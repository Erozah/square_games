package fr.games.square.api.dto;

import java.util.UUID;

public record TokenResponseDto(
        String name,
        UUID ownerId,
        int x,
        int y,
        boolean canMove
) {}
