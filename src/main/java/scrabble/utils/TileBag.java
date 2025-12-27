package scrabble.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TileBag {
    private List<Tile> tiles;
    private final Random random;

    public static class Tile {
        private final String id;
        private final char letter;
        private final int points;

        public Tile(char letter, int points) {
            this.letter = letter;
            this.points = points;
            this.id = generateId();
        }

        private String generateId() {
            return letter + "_" + System.currentTimeMillis() + "_" + (new Random()).nextInt(1000);
        }

        public char getLetter() {
            return letter;
        }

        public int getPoints() {
            return points;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return String.valueOf(letter).toUpperCase() + "(" + points + ")";
        }
    }

    public TileBag() {
        this.random = new Random();
        initializeEnglishTiles();
    }

    private void initializeEnglishTiles() {
        tiles = new ArrayList<>();


        addTile('A', 1, 9);
        addTile('B', 3, 2);
        addTile('C', 3, 2);
        addTile('D', 2, 4);
        addTile('E', 1, 12);
        addTile('F', 4, 2);
        addTile('G', 2, 3);
        addTile('H', 4, 2);
        addTile('I', 1, 9);
        addTile('J', 8, 1);
        addTile('K', 5, 1);
        addTile('L', 1, 4);
        addTile('M', 3, 2);
        addTile('N', 1, 6);
        addTile('O', 1, 8);
        addTile('P', 3, 2);
        addTile('Q', 10, 1);
        addTile('R', 1, 6);
        addTile('S', 1, 4);
        addTile('T', 1, 6);
        addTile('U', 1, 4);
        addTile('V', 4, 2);
        addTile('W', 4, 2);
        addTile('X', 8, 1);
        addTile('Y', 4, 2);
        addTile('Z', 10, 1);
        addTile(' ', 0, 2);

        System.out.println("Initialized bag with " + tiles.size() + " English tiles");
        shuffle();
    }

    private void addTile(char letter, int points, int count) {
        for (int i = 0; i < count; i++) {
            tiles.add(new Tile(letter, points));
        }
    }

    public void shuffle() {
        Collections.shuffle(tiles, random);
    }

    public synchronized Tile drawTile() {
        if (tiles.isEmpty()) {
            return null;
        }
        return tiles.remove(tiles.size() - 1);
    }

    public synchronized void returnTile(Tile tile) {
        tiles.add(tile);
        shuffle();
    }

    public synchronized int remainingTiles() {
        return tiles.size();
    }

    public synchronized List<Tile> drawTiles(int count) {
        List<Tile> drawn = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Tile tile = drawTile();
            if (tile != null) {
                drawn.add(tile);
            } else {
                break;
            }
        }
        return drawn;
    }


    public static int getLetterValue(char letter) {
        char upperLetter = Character.toUpperCase(letter);
        return switch (upperLetter) {
            case 'A', 'E', 'I', 'O', 'U', 'L', 'N', 'S', 'T', 'R' -> 1;
            case 'D', 'G' -> 2;
            case 'B', 'C', 'M', 'P' -> 3;
            case 'F', 'H', 'V', 'W', 'Y' -> 4;
            case 'K' -> 5;
            case 'J', 'X' -> 8;
            case 'Q', 'Z' -> 10;
            default -> 0;
        };
    }


    public static boolean isVowel(char letter) {
        char upperLetter = Character.toUpperCase(letter);
        return upperLetter == 'A' || upperLetter == 'E' || upperLetter == 'I' || upperLetter == 'O'
                || upperLetter == 'U' || upperLetter == 'Y';
    }


    public static boolean isConsonant(char letter) {
        char upperLetter = Character.toUpperCase(letter);
        return !isVowel(upperLetter) && upperLetter >= 'A' && upperLetter <= 'Z';
    }
}
