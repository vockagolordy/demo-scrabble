package scrabble.client.view.components;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import scrabble.client.model.GameState;
import scrabble.client.view.components.TileView.TileDropEvent;
import scrabble.utils.TileBag;

public class BoardCanvas extends Canvas {
    private static final int CELL_SIZE = 40;
    private static final int BOARD_SIZE = 15;

    private GameState gameState;
    private TileBag.Tile draggedTile;
    private int dragCellRow = -1;
    private int dragCellCol = -1;

    public BoardCanvas() {
        super(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE);
        setWidth(BOARD_SIZE * CELL_SIZE);
        setHeight(BOARD_SIZE * CELL_SIZE);

        // Обработка событий мыши для drag & drop
        setupMouseHandlers();
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        drawBoard();
    }

    public void drawBoard() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        // Рисуем сетку
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                drawCell(gc, row, col);
            }
        }

        // Рисуем координаты
        drawCoordinates(gc);

        // Рисуем фишку, которую перетаскиваем
        if (draggedTile != null && dragCellRow >= 0 && dragCellCol >= 0) {
            drawTileAt(gc, dragCellRow, dragCellCol, draggedTile, true);
        }
    }

    private void drawCell(GraphicsContext gc, int row, int col) {
        double x = col * CELL_SIZE;
        double y = row * CELL_SIZE;

        // Фон клетки
        Color cellColor = getCellColor(row, col);
        gc.setFill(cellColor);
        gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);

        // Граница клетки
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(0.5);
        gc.strokeRect(x, y, CELL_SIZE, CELL_SIZE);

        // Бонусные обозначения
        String bonus = getBonusText(row, col);
        if (bonus != null) {
            gc.setFill(Color.DARKBLUE);
            gc.setFont(Font.font(10));
            gc.fillText(bonus, x + CELL_SIZE/4, y + CELL_SIZE*3/4);
        }

        // Если есть фишка, рисуем её
        if (gameState != null && gameState.getCell(row, col) != null
                && gameState.getCell(row, col).hasTile()) {
            drawTileAt(gc, row, col, gameState.getCell(row, col).getTile(), false);
        }
    }

    private Color getCellColor(int row, int col) {
        // Центр - красный
        if (row == 7 && col == 7) return Color.LIGHTCORAL;

        // Утроение слова - красный
        if ((row == 0 || row == 14) && (col == 0 || col == 14) ||
                (row == 0 && col == 7) || (row == 7 && col == 0) ||
                (row == 7 && col == 14) || (row == 14 && col == 7)) {
            return Color.INDIANRED;
        }

        // Удвоение слова - розовый
        if ((row == 1 || row == 13) && (col == 1 || col == 13) ||
                (row == 2 || row == 12) && (col == 2 || col == 12) ||
                (row == 3 || row == 11) && (col == 3 || col == 11) ||
                (row == 4 || row == 10) && (col == 4 || col == 10)) {
            return Color.LIGHTPINK;
        }

        // Удвоение буквы - голубой
        if (row == col || row == 14 - col)
            return Color.LIGHTBLUE;

        // Утроение буквы - синий
        if ((row == 1 || row == 13) && (col == 5 || col == 9) ||
                (row == 5 || row == 9) && (col == 1 || col == 13) ||
                (row == 5 || row == 9) && (col == 5 || col == 9)) {
            return Color.LIGHTSTEELBLUE;
        }

        // Остальные клетки - бежевые
        return Color.BEIGE;
    }

    private String getBonusText(int row, int col) {
        if (row == 7 && col == 7) return "★";
        if ((row == 0 || row == 14) && (col == 0 || col == 14) ||
                (row == 0 && col == 7) || (row == 7 && col == 0) ||
                (row == 7 && col == 14) || (row == 14 && col == 7)) return "3x";
        if ((row == 1 || row == 13) && (col == 1 || col == 13) ||
                (row == 2 || row == 12) && (col == 2 || col == 12) ||
                (row == 3 || row == 11) && (col == 3 || col == 11) ||
                (row == 4 || row == 10) && (col == 4 || col == 10)) return "2x";
        if ((row == 1 || row == 13) && (col == 5 || col == 9) ||
                (row == 5 || row == 9) && (col == 1 || col == 13) ||
                (row == 5 || row == 9) && (col == 5 || col == 9)) return "3л";
        if (row == col || row == 14 - col) return "2л";
        return null;
    }

    private void drawTileAt(GraphicsContext gc, int row, int col, TileBag.Tile tile, boolean isDragged) {
        double x = col * CELL_SIZE;
        double y = row * CELL_SIZE;

        // Фон фишки
        Color fillColor = isDragged ? Color.GOLD.deriveColor(0, 1, 1, 0.7) : Color.GOLD;
        gc.setFill(fillColor);
        gc.fillRoundRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4, 8, 8);

        // Рамка фишки
        gc.setStroke(isDragged ? Color.DARKORANGE : Color.DARKGOLDENROD);
        gc.setLineWidth(isDragged ? 3 : 2);
        gc.strokeRoundRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4, 8, 8);

        // Буква
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        String letter = String.valueOf(tile.getLetter()).toUpperCase();
        gc.fillText(letter, x + CELL_SIZE/2 - 5, y + CELL_SIZE/2 + 5);

        // Очки
        gc.setFill(Color.DARKRED);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        gc.fillText(String.valueOf(tile.getPoints()), x + CELL_SIZE - 12, y + CELL_SIZE - 5);
    }

    private void drawCoordinates(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font(10));

        for (int i = 0; i < BOARD_SIZE; i++) {
            // Номера строк
            gc.fillText(String.valueOf(i + 1), 2, i * CELL_SIZE + 12);

            // Буквы столбцов
            char colChar = (char) ('A' + i);
            gc.fillText(String.valueOf(colChar), i * CELL_SIZE + 15, 10);
        }
    }

    private void setupMouseHandlers() {
        setOnMouseDragged(event -> {
            if (draggedTile != null) {
                int col = (int) (event.getX() / CELL_SIZE);
                int row = (int) (event.getY() / CELL_SIZE);

                if (col >= 0 && col < BOARD_SIZE && row >= 0 && row < BOARD_SIZE) {
                    dragCellRow = row;
                    dragCellCol = col;
                    drawBoard();
                }
            }
            event.consume();
        });

        setOnMouseReleased(event -> {
            if (draggedTile != null && dragCellRow >= 0 && dragCellCol >= 0) {
                // Создаем событие размещения фишки
                TileDropEvent dropEvent = new TileDropEvent(
                        TileDropEvent.TILE_DROPPED,
                        draggedTile,
                        dragCellRow,
                        dragCellCol
                );
                fireEvent(dropEvent);

                // Сбрасываем состояние перетаскивания
                draggedTile = null;
                dragCellRow = -1;
                dragCellCol = -1;
                drawBoard();
            }
            event.consume();
        });

        setOnMouseExited(event -> {
            draggedTile = null;
            drawBoard();
        });
    }

    public void setDraggedTile(TileBag.Tile tile) {
        this.draggedTile = tile;
        this.dragCellRow = -1;
        this.dragCellCol = -1;
        drawBoard();
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void resize(double width, double height) {
        setWidth(width);
        setHeight(height);
        drawBoard();
    }
}