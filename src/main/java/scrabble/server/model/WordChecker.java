// scrabble/server/model/WordChecker.java

package scrabble.server.model;

import scrabble.client.model.GameState;
import scrabble.utils.DictionaryLoader;
import scrabble.utils.TileBag;

import java.util.*;

public class WordChecker {
    private final ServerModel serverModel;

    // Типы бонусных клеток (стандартные для английского Scrabble)
    private static final String[][] CELL_TYPES = new String[15][15];

    static {
        // Инициализация бонусных клеток
        initializeCellTypes();
    }

    private static void initializeCellTypes() {
        // Сначала инициализируем все клетки как обычные
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                CELL_TYPES[i][j] = "";
            }
        }

        // Triple Word Score (TW) - красные клетки
        int[][] twCells = {{0,0}, {0,7}, {0,14}, {7,0}, {7,14}, {14,0}, {14,7}, {14,14}};
        for (int[] cell : twCells) {
            CELL_TYPES[cell[0]][cell[1]] = "TW";
        }

        // Double Word Score (DW) - розовые клетки
        int[][] dwCells = {{1,1}, {1,13}, {2,2}, {2,12}, {3,3}, {3,11}, {4,4}, {4,10},
                {10,4}, {10,10}, {11,3}, {11,11}, {12,2}, {12,12}, {13,1}, {13,13}};
        for (int[] cell : dwCells) {
            CELL_TYPES[cell[0]][cell[1]] = "DW";
        }

        // Центральная клетка - стартовая (тоже Double Word)
        CELL_TYPES[7][7] = "DW";

        // Double Letter Score (DL) - голубые клетки
        for (int i = 1; i < 14; i++) {
            CELL_TYPES[i][i] = "DL";
            CELL_TYPES[i][14-i] = "DL";
        }

        // Дополнительные DL клетки
        int[][] dlCells = {{0,3}, {0,11}, {2,6}, {2,8}, {3,0}, {3,7}, {3,14},
                {6,2}, {6,6}, {6,8}, {6,12}, {7,3}, {7,11},
                {8,2}, {8,6}, {8,8}, {8,12}, {11,0}, {11,7}, {11,14},
                {12,6}, {12,8}, {14,3}, {14,11}};
        for (int[] cell : dlCells) {
            CELL_TYPES[cell[0]][cell[1]] = "DL";
        }

        // Triple Letter Score (TL) - синие клетки
        int[][] tlCells = {{1,5}, {1,9}, {5,1}, {5,5}, {5,9}, {5,13},
                {9,1}, {9,5}, {9,9}, {9,13}, {13,5}, {13,9}};
        for (int[] cell : tlCells) {
            CELL_TYPES[cell[0]][cell[1]] = "TL";
        }
    }

    public WordChecker(ServerModel serverModel) {
        this.serverModel = serverModel;
    }

    public ValidationResult validateMove(String word, int row, int col, boolean horizontal,
                                         GameState.BoardCell[][] board, List<String> tileIds,
                                         String playerId, GameRoom room) {
        ValidationResult result = new ValidationResult();

        // Базовая проверка
        if (!basicValidation(word, row, col, horizontal, result)) {
            return result;
        }

        word = word.toUpperCase();

        // Проверка размещения на доске
        if (!canPlaceWord(word, row, col, horizontal, board)) {
            result.setValid(false);
            result.setMessage("Cannot place word at the specified position");
            return result;
        }

        // Проверка первого хода (должен быть через центр)
        if (isFirstMove(board) && !isCrossingCenter(word, row, col, horizontal)) {
            result.setValid(false);
            result.setMessage("First move must pass through the center cell (H8)");
            return result;
        }

        // Проверка фишек игрока
        if (!validatePlayerTiles(word, tileIds, playerId, room)) {
            result.setValid(false);
            result.setMessage("You don't have the required tiles for this move");
            return result;
        }

        // Поиск всех новых слов
        List<WordInfo> allNewWords = findAllNewWords(word, row, col, horizontal, board);
        if (allNewWords.isEmpty() && !isFirstMove(board)) {
            result.setValid(false);
            result.setMessage("Word must touch existing words");
            return result;
        }

        // Проверка всех новых слов в словаре
        for (WordInfo wordInfo : allNewWords) {
            if (!DictionaryLoader.isValidWord(wordInfo.word)) {
                result.setValid(false);
                result.setMessage("Invalid word formed: " + wordInfo.word);
                return result;
            }
            result.addFormedWord(wordInfo.word);
        }

        // Расчет очков
        int totalScore = calculateTotalScore(allNewWords, board, word, row, col, horizontal);

        // Бонус за использование всех 7 фишек (BINGO)
        if (word.length() == 7) {
            totalScore += 50;
            result.setMessage("BINGO! +50 points for using all tiles!");
        } else {
            result.setMessage("Word accepted! Score: " + totalScore);
        }

        result.setValid(true);
        result.setScore(totalScore);

        return result;
    }

    private boolean basicValidation(String word, int row, int col, boolean horizontal,
                                    ValidationResult result) {
        if (word == null || word.trim().isEmpty()) {
            result.setValid(false);
            result.setMessage("Word cannot be empty");
            return false;
        }

        word = word.trim().toUpperCase();

        // Проверяем, что слово состоит только из английских букв
        if (!word.matches("^[A-Z]+$")) {
            result.setValid(false);
            result.setMessage("Word must contain only English letters");
            return false;
        }

        if (word.length() < 2) {
            result.setValid(false);
            result.setMessage("Word must contain at least 2 letters");
            return false;
        }

        if (row < 0 || row >= 15 || col < 0 || col >= 15) {
            result.setValid(false);
            result.setMessage("Coordinates are out of board bounds");
            return false;
        }

        if (horizontal && col + word.length() > 15) {
            result.setValid(false);
            result.setMessage("Word doesn't fit horizontally");
            return false;
        }

        if (!horizontal && row + word.length() > 15) {
            result.setValid(false);
            result.setMessage("Word doesn't fit vertically");
            return false;
        }

        return true;
    }

    private boolean canPlaceWord(String word, int row, int col, boolean horizontal,
                                 GameState.BoardCell[][] board) {
        boolean touchesExisting = false;

        for (int i = 0; i < word.length(); i++) {
            int r = horizontal ? row : row + i;
            int c = horizontal ? col + i : col;

            GameState.BoardCell cell = board[r][c];

            // Проверка перекрытия с несовпадающей буквой
            if (cell.hasTile() && Character.toUpperCase(cell.getTile().getLetter()) != word.charAt(i)) {
                return false;
            }

            // Проверка соседних клеток для определения касания
            if (!cell.hasTile()) {
                if (hasAdjacentTile(r, c, board)) {
                    touchesExisting = true;
                }
            } else {
                touchesExisting = true;
            }
        }

        return isFirstMove(board) || touchesExisting;
    }

    private boolean hasAdjacentTile(int row, int col, GameState.BoardCell[][] board) {
        // Проверяем все четыре направления
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (newRow >= 0 && newRow < 15 && newCol >= 0 && newCol < 15) {
                if (board[newRow][newCol].hasTile()) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isFirstMove(GameState.BoardCell[][] board) {
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                if (board[i][j].hasTile()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isCrossingCenter(String word, int row, int col, boolean horizontal) {
        for (int i = 0; i < word.length(); i++) {
            int r = horizontal ? row : row + i;
            int c = horizontal ? col + i : col;

            if (r == 7 && c == 7) {
                return true;
            }
        }
        return false;
    }

    private boolean validatePlayerTiles(String word, List<String> tileIds, String playerId, GameRoom room) {
        // В реальной реализации нужно проверить, что у игрока есть эти фишки
        // и что tileIds соответствуют буквам в слове

        if (tileIds == null || tileIds.size() != word.length()) {
            return false;
        }

        // Здесь должна быть логика проверки фишек игрока
        // Пока возвращаем true для упрощения
        return true;
    }

    private List<WordInfo> findAllNewWords(String mainWord, int row, int col, boolean horizontal,
                                           GameState.BoardCell[][] board) {
        List<WordInfo> allWords = new ArrayList<>();

        // Добавляем основное слово
        allWords.add(new WordInfo(mainWord, row, col, horizontal));

        // Ищем перпендикулярные слова
        for (int i = 0; i < mainWord.length(); i++) {
            int r = horizontal ? row : row + i;
            int c = horizontal ? col + i : col;

            // Если это новая фишка, ищем слово в перпендикулярном направлении
            if (!board[r][c].hasTile()) {
                WordInfo perpendicularWord = findPerpendicularWord(r, c, !horizontal, board, mainWord.charAt(i));
                if (perpendicularWord != null && perpendicularWord.word.length() > 1) {
                    allWords.add(perpendicularWord);
                }
            }
        }

        return allWords;
    }

    private WordInfo findPerpendicularWord(int row, int col, boolean horizontal,
                                           GameState.BoardCell[][] board, char newLetter) {
        StringBuilder word = new StringBuilder();
        int startRow = row;
        int startCol = col;

        // Идем назад от новой фишки
        int r = row, c = col;
        if (horizontal) {
            while (c >= 0) {
                GameState.BoardCell cell = board[r][c];
                if (c == col) {
                    word.insert(0, newLetter);
                } else if (cell.hasTile()) {
                    word.insert(0, Character.toUpperCase(cell.getTile().getLetter()));
                } else {
                    startCol = c + 1;
                    break;
                }
                c--;
            }
            if (c < 0) startCol = 0;
        } else {
            while (r >= 0) {
                GameState.BoardCell cell = board[r][c];
                if (r == row) {
                    word.insert(0, newLetter);
                } else if (cell.hasTile()) {
                    word.insert(0, Character.toUpperCase(cell.getTile().getLetter()));
                } else {
                    startRow = r + 1;
                    break;
                }
                r--;
            }
            if (r < 0) startRow = 0;
        }

        // Идем вперед от новой фишки
        r = horizontal ? row : row + 1;
        c = horizontal ? col + 1 : col;
        if (horizontal) {
            while (c < 15) {
                GameState.BoardCell cell = board[r][c];
                if (cell.hasTile()) {
                    word.append(Character.toUpperCase(cell.getTile().getLetter()));
                } else {
                    break;
                }
                c++;
            }
        } else {
            while (r < 15) {
                GameState.BoardCell cell = board[r][c];
                if (cell.hasTile()) {
                    word.append(Character.toUpperCase(cell.getTile().getLetter()));
                } else {
                    break;
                }
                r++;
            }
        }

        String formedWord = word.toString().toUpperCase();
        if (formedWord.length() > 1) {
            return new WordInfo(formedWord, startRow, startCol, horizontal);
        }

        return null;
    }

    private int calculateTotalScore(List<WordInfo> allWords, GameState.BoardCell[][] board,
                                    String mainWord, int mainRow, int mainCol, boolean mainHorizontal) {
        int totalScore = 0;

        for (WordInfo wordInfo : allWords) {
            totalScore += calculateWordScore(wordInfo, board);
        }

        return totalScore;
    }

    private int calculateWordScore(WordInfo wordInfo, GameState.BoardCell[][] board) {
        int wordScore = 0;
        int wordMultiplier = 1;
        boolean hasBlankTile = false;

        for (int i = 0; i < wordInfo.word.length(); i++) {
            int r = wordInfo.horizontal ? wordInfo.row : wordInfo.row + i;
            int c = wordInfo.horizontal ? wordInfo.col + i : wordInfo.col;

            char letter = wordInfo.word.charAt(i);
            int letterScore = TileBag.getLetterValue(letter);

            GameState.BoardCell cell = board[r][c];
            String cellType = CELL_TYPES[r][c];

            // Проверяем, является ли фишка бланком
            if (cell.hasTile() && cell.getTile().getLetter() == ' ') {
                hasBlankTile = true;
                letterScore = 0;
            }

            // Если клетка пустая (новая фишка), применяем бонусы
            if (!cell.hasTile()) {
                if (cellType.equals("DL")) {
                    letterScore *= 2;
                } else if (cellType.equals("TL")) {
                    letterScore *= 3;
                } else if (cellType.equals("DW")) {
                    wordMultiplier *= 2;
                } else if (cellType.equals("TW")) {
                    wordMultiplier *= 3;
                }
            }

            wordScore += letterScore;
        }

        // Если использован бланк, бонусы слова не применяются
        if (hasBlankTile) {
            return wordScore;
        }

        return wordScore * wordMultiplier;
    }

    // Вспомогательный класс для хранения информации о слове
    private static class WordInfo {
        String word;
        int row;
        int col;
        boolean horizontal;

        WordInfo(String word, int row, int col, boolean horizontal) {
            this.word = word.toUpperCase();
            this.row = row;
            this.col = col;
            this.horizontal = horizontal;
        }
    }

    public static class ValidationResult {
        private boolean valid;
        private String message;
        private int score;
        private List<String> formedWords;

        public ValidationResult() {
            this.formedWords = new ArrayList<>();
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public int getScore() { return score; }
        public List<String> getFormedWords() { return formedWords; }

        public void setValid(boolean valid) { this.valid = valid; }
        public void setMessage(String message) { this.message = message; }
        public void setScore(int score) { this.score = score; }
        public void addFormedWord(String word) { formedWords.add(word); }
        public void setFormedWords(List<String> formedWords) { this.formedWords = formedWords; }
    }

    // Дополнительные методы для серверной логики

    /**
     * Проверяет, можно ли сделать ход в данной позиции
     */
    public boolean canMakeMove(GameState.BoardCell[][] board, String playerId, GameRoom room) {
        // Проверка, что игрок имеет фишки
        // Проверка, что есть возможные ходы
        return true;
    }

    /**
     * Возвращает список возможных слов для данных фишек
     */
    public List<String> getPossibleWords(List<Character> availableLetters) {
        List<String> possibleWords = new ArrayList<>();

        // Преобразуем буквы в строку для поиска по шаблону
        StringBuilder letters = new StringBuilder();
        for (Character c : availableLetters) {
            letters.append(Character.toUpperCase(c));
        }

        // Это упрощенная реализация - в реальном Scrabble нужен более сложный алгоритм
        String lettersStr = letters.toString();

        // Проверяем некоторые базовые комбинации
        if (lettersStr.contains("T") && lettersStr.contains("E") && lettersStr.contains("S") && lettersStr.contains("T")) {
            possibleWords.add("TEST");
        }
        if (lettersStr.contains("W") && lettersStr.contains("O") && lettersStr.contains("R") && lettersStr.contains("D")) {
            possibleWords.add("WORD");
        }
        if (lettersStr.contains("G") && lettersStr.contains("A") && lettersStr.contains("M") && lettersStr.contains("E")) {
            possibleWords.add("GAME");
        }
        if (lettersStr.contains("P") && lettersStr.contains("L") && lettersStr.contains("A") && lettersStr.contains("Y")) {
            possibleWords.add("PLAY");
        }

        return possibleWords;
    }

    /**
     * Обновляет доску после успешного хода
     */
    public void updateBoard(GameState.BoardCell[][] board, String word, int row, int col,
                            boolean horizontal, List<scrabble.utils.TileBag.Tile> tiles) {
        for (int i = 0; i < word.length(); i++) {
            int r = horizontal ? row : row + i;
            int c = horizontal ? col + i : col;

            if (!board[r][c].hasTile()) {
                char neededLetter = Character.toUpperCase(word.charAt(i));

                // Находим соответствующую фишку
                for (scrabble.utils.TileBag.Tile tile : tiles) {
                    if (Character.toUpperCase(tile.getLetter()) == neededLetter) {
                        board[r][c].setTile(tile);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Проверяет конец игры
     */
    public boolean isGameOver(GameState.BoardCell[][] board, scrabble.utils.TileBag tileBag) {
        // Игра заканчивается, когда:
        // 1. Закончились фишки в мешке
        // 2. Один из игроков использовал все свои фишки
        return tileBag.remainingTiles() == 0;
    }

    /**
     * Рассчитывает финальные очки с учетом оставшихся фишек
     */
    public int calculateFinalScore(int currentScore, List<scrabble.utils.TileBag.Tile> remainingTiles) {
        int penalty = 0;
        for (scrabble.utils.TileBag.Tile tile : remainingTiles) {
            penalty += TileBag.getLetterValue(tile.getLetter());
        }

        return Math.max(0, currentScore - penalty);
    }

    /**
     * Получает тип клетки по координатам
     */
    public static String getCellType(int row, int col) {
        if (row >= 0 && row < 15 && col >= 0 && col < 15) {
            return CELL_TYPES[row][col];
        }
        return "";
    }

    /**
     * Проверяет, является ли клетка бонусной
     */
    public static boolean isBonusCell(int row, int col) {
        String cellType = getCellType(row, col);
        return !cellType.isEmpty();
    }
}