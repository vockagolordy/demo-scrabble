package scrabble.client.view.components;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import scrabble.utils.TileBag;
import java.util.ArrayList;
import java.util.List;

public class RackView extends Pane {
    private static final double TILE_SPACING = 5;
    private static final double RACK_HEIGHT = 70;
    private static final double RACK_WIDTH = 7 * 50 + 6 * TILE_SPACING;

    private List<TileView> tileViews;
    private Rectangle rackBackground;

    public RackView() {
        tileViews = new ArrayList<>();
        initializeView();
    }

    private void initializeView() {
        setPrefSize(RACK_WIDTH, RACK_HEIGHT);

        
        rackBackground = new Rectangle(RACK_WIDTH, RACK_HEIGHT);
        rackBackground.setArcWidth(15);
        rackBackground.setArcHeight(15);
        rackBackground.setFill(Color.SADDLEBROWN);
        rackBackground.setStroke(Color.SIENNA);
        rackBackground.setStrokeWidth(2);

        getChildren().add(rackBackground);
    }

    public void addTile(TileBag.Tile tile) {
        TileView tileView = new TileView(tile);
        tileViews.add(tileView);
        updateTilePositions();
        getChildren().add(tileView);
    }

    public void removeTile(String tileId) {
        tileViews.removeIf(tileView -> {
            if (tileView.getTile().getId().equals(tileId)) {
                getChildren().remove(tileView);
                return true;
            }
            return false;
        });
        updateTilePositions();
    }

    public void clearTiles() {
        getChildren().removeAll(tileViews);
        tileViews.clear();
    }

    public List<TileBag.Tile> getTiles() {
        List<TileBag.Tile> tiles = new ArrayList<>();
        for (TileView tileView : tileViews) {
            tiles.add(tileView.getTile());
        }
        return tiles;
    }

    private void updateTilePositions() {
        double totalWidth = tileViews.size() * 50 + (tileViews.size() - 1) * TILE_SPACING;
        double startX = (RACK_WIDTH - totalWidth) / 2;

        for (int i = 0; i < tileViews.size(); i++) {
            TileView tileView = tileViews.get(i);
            tileView.setLayoutX(startX + i * (50 + TILE_SPACING));
            tileView.setLayoutY((RACK_HEIGHT - 50) / 2);
        }
    }

    public void setOnTileDropped(javafx.event.EventHandler<TileView.TileDropEvent> handler) {
        for (TileView tileView : tileViews) {
            tileView.addEventHandler(TileView.TileDropEvent.TILE_DROPPED, handler);
        }
    }
}