package scrabble.protocol;

public enum MessageType {
    CONNECT,           // Подключение к серверу
    CREATE_ROOM,       // Создание комнаты
    JOIN_ROOM,         // Вход в комнату
    LEAVE_ROOM,        // Выход из комнаты
    PLAYER_JOINED,     // Игрок присоединился к комнате
    PLAYER_LEFT,       // Игрок покинул комнату
    PLAYER_READY,      // Игрок готов
    ALL_PLAYERS_READY, // Все игроки готовы
    GAME_START,        // Начало игры
    GAME_STATE,        // Состояние игры
    PLAYER_MOVE,       // Ход игрока
    TILES_EXCHANGE,    // Обмен фишек
    CHAT_MESSAGE,      // Сообщение в чат
    ROOM_LIST,         // Список комнат
    GAME_OVER,         // Конец игры
    ERROR,             // Ошибка
    DISCONNECT         // Отключение
}