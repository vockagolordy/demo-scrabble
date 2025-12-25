package scrabble.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TileBag {
    private List<Tile> tiles;
    private Random random;

    public static class Tile {
        private String id;
        private char letter;
        private int points;

        public Tile(char letter, int points) {
            this.letter = letter;
            this.points = points;
            this.id = generateId();
        }

        private String generateId() {
            return letter + "_" + System.currentTimeMillis() + "_" + (new Random()).nextInt(1000);
        }

        public char getLetter() { return letter; }
        public int getPoints() { return points; }
        public String getId() { return id; }

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

        // Стандартное английское распределение фишек для Scrabble
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
        addTile(' ', 0, 2); // Blank tiles (wildcards)

        System.out.println("Инициализирован мешок с " + tiles.size() + " английскими фишками");
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

    // Метод для получения стоимости буквы (для WordChecker)
    public static int getLetterValue(char letter) {
        letter = Character.toUpperCase(letter);
        switch (letter) {
            case 'A': case 'E': case 'I': case 'O': case 'U':
            case 'L': case 'N': case 'S': case 'T': case 'R':
                return 1;
            case 'D': case 'G':
                return 2;
            case 'B': case 'C': case 'M': case 'P':
                return 3;
            case 'F': case 'H': case 'V': case 'W': case 'Y':
                return 4;
            case 'K':
                return 5;
            case 'J': case 'X':
                return 8;
            case 'Q': case 'Z':
                return 10;
            case ' ': // Blank tile
                return 0;
            default:
                return 0;
        }
    }

    // Метод для проверки, является ли буква гласной (может пригодиться)
    public static boolean isVowel(char letter) {
        letter = Character.toUpperCase(letter);
        return letter == 'A' || letter == 'E' || letter == 'I' || letter == 'O' || letter == 'U' || letter == 'Y';
    }

    // Метод для проверки, является ли буква согласной
    public static boolean isConsonant(char letter) {
        letter = Character.toUpperCase(letter);
        return !isVowel(letter) && letter >= 'A' && letter <= 'Z';
    }
}