package scrabble.client.view.components;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import scrabble.utils.TileBag;
import javafx.event.EventType;

public class TileView extends StackPane {
    private static final double SIZE = 50;
    private static final double FONT_SIZE = 20;

    private TileBag.Tile tile;
    private Rectangle background;
    private Text letterText;
    private Text pointsText;
    private boolean isDragging;
    private double dragStartX, dragStartY;

    public TileView(TileBag.Tile tile) {
        this.tile = tile;
        this.isDragging = false;

        initializeView();
        setupDragAndDrop();
    }

    private void initializeView() {
        
        background = new Rectangle(SIZE, SIZE);
        background.setArcWidth(10);
        background.setArcHeight(10);
        background.setFill(Color.GOLD);
        background.setStroke(Color.DARKGOLDENROD);
        background.setStrokeWidth(2);

        
        letterText = new Text(String.valueOf(tile.getLetter()).toUpperCase());
        letterText.setFont(Font.font("Arial", FontWeight.BOLD, FONT_SIZE));
        letterText.setFill(Color.BLACK);

        
        pointsText = new Text(String.valueOf(tile.getPoints()));
        pointsText.setFont(Font.font("Arial", FontWeight.NORMAL, FONT_SIZE * 0.6));
        pointsText.setFill(Color.DARKRED);

        
        letterText.setTranslateY(-SIZE * 0.1);
        pointsText.setTranslateX(SIZE * 0.3);
        pointsText.setTranslateY(SIZE * 0.3);

        getChildren().addAll(background, letterText, pointsText);
    }

    private void setupDragAndDrop() {
        setOnMousePressed(event -> {
            isDragging = true;
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
            setMouseTransparent(true);
            event.consume();
        });

        setOnMouseDragged(event -> {
            if (isDragging) {
                double deltaX = event.getSceneX() - dragStartX;
                double deltaY = event.getSceneY() - dragStartY;
                setTranslateX(deltaX);
                setTranslateY(deltaY);
                event.consume();
            }
        });

        setOnMouseReleased(event -> {
            if (isDragging) {
                isDragging = false;
                setMouseTransparent(false);
                setTranslateX(0);
                setTranslateY(0);

                
                TileDropEvent dropEvent = new TileDropEvent(TileDropEvent.TILE_DROPPED, tile,
                        event.getSceneX(), event.getSceneY());
                fireEvent(dropEvent);
                event.consume();
            }
        });
    }

    public TileBag.Tile getTile() {
        return tile;
    }

    public void setSelected(boolean selected) {
        if (selected) {
            background.setStroke(Color.RED);
            background.setStrokeWidth(3);
        } else {
            background.setStroke(Color.DARKGOLDENROD);
            background.setStrokeWidth(2);
        }
    }

    
    public static class TileDropEvent extends javafx.event.Event {
        public static final EventType<TileDropEvent> TILE_DROPPED =
                new EventType<>(javafx.event.Event.ANY, "TILE_DROPPED");

        private final TileBag.Tile tile;
        private final double sceneX;
        private final double sceneY;

        public TileDropEvent(EventType<? extends javafx.event.Event> eventType,
                             TileBag.Tile tile, double sceneX, double sceneY) {
            super(eventType);
            this.tile = tile;
            this.sceneX = sceneX;
            this.sceneY = sceneY;
        }

        public TileBag.Tile getTile() {
            return tile;
        }

        public double getSceneX() {
            return sceneX;
        }

        public double getSceneY() {
            return sceneY;
        }
    }
}