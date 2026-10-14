package fr.games.square.api.game;

import jakarta.validation.constraints.NotNull;

public class MoveParams {

    @NotNull(message = "La coordonnée x est requise")
    private Integer x;

    @NotNull(message = "La coordonnée y est requise")
    private Integer y;

    public MoveParams() {
    }

    public MoveParams(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }
}
