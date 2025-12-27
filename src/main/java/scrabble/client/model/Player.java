package scrabble.client.model;

import java.util.ArrayList;
import java.util.List;
import scrabble.utils.TileBag.Tile;

public class Player {
    private String id;
    private String name;
    private int score;
    private List<Tile> rack;
    private boolean ready;
    private boolean isCurrentTurn;

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.score = 0;
        this.rack = new ArrayList<>();
        this.ready = false;
        this.isCurrentTurn = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getScore() { return score; }
    public List<Tile> getRack() { return rack; }
    public boolean isReady() { return ready; }
    public boolean isCurrentTurn() { return isCurrentTurn; }

    public void setScore(int score) { this.score = score; }
    public void setReady(boolean ready) { this.ready = ready; }
    public void setCurrentTurn(boolean isCurrentTurn) { this.isCurrentTurn = isCurrentTurn; }

    public void addToRack(Tile tile) {
        if (rack.size() < 7) {
            rack.add(tile);
        }
    }

    public void removeFromRack(String tileId) {
        rack.removeIf(tile -> tile.getId().equals(tileId));
    }

    public Tile getTileById(String tileId) {
        return rack.stream()
                .filter(tile -> tile.getId().equals(tileId))
                .findFirst()
                .orElse(null);
    }

    public void addScore(int points) {
        this.score += points;
    }
}