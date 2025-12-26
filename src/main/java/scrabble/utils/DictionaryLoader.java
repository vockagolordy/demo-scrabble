// scrabble/utils/DictionaryLoader.java

package scrabble.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class DictionaryLoader {
    private static Set<String> dictionary;
    private static final String DICTIONARY_FILE = "/dictionary.txt";

    public static synchronized Set<String> loadDictionary() {
        if (dictionary == null) {
            dictionary = new HashSet<>();
            loadDictionaryFromFile();
        }
        return dictionary;
    }

    private static void loadDictionaryFromFile() {
        int loadedCount = 0;
        try (InputStream is = DictionaryLoader.class.getResourceAsStream(DICTIONARY_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim().toUpperCase();
                if (!word.isEmpty() && word.matches("^\"[A-Z]+\"$")) {
                    dictionary.add(word);
                    loadedCount++;

                    // Для отладки выводим прогресс каждые 10000 слов
                    if (loadedCount % 10000 == 0) {
                        System.out.println("Загружено " + loadedCount + " английских слов...");
                    }
                }
            }
            System.out.println("Успешно загружено " + loadedCount + " английских слов из словаря");

        } catch (Exception e) {
            System.err.println("Ошибка загрузки английского словаря: " + e.getMessage());
            System.err.println("Проверьте наличие файла " + DICTIONARY_FILE + " в resources");

            // В случае ошибки создаем минимальный словарь для тестирования
            createFallbackDictionary();
        }
    }

    private static void createFallbackDictionary() {
        System.out.println("Создание резервного английского словаря...");
        String[] commonEnglishWords = {
                "A", "I",
                "ABOUT", "ALL", "ALSO", "AND", "AS", "AT", "BE", "BECAUSE", "BUT", "BY", "CAN", "COME", "COULD", "DAY", "DO", "EVEN", "FIND", "FIRST", "FOR", "FROM", "GET", "GIVE", "GO", "HAVE", "HE", "HER", "HERE", "HIM", "HIS", "HOW", "IF", "IN", "INTO", "IT", "ITS", "JUST", "KNOW", "LIKE", "LOOK", "MAKE", "MAN", "MANY", "ME", "MORE", "MY", "NEW", "NO", "NOT", "NOW", "OF", "ON", "ONE", "ONLY", "OR", "OTHER", "OUR", "OUT", "OVER", "PEOPLE", "SAY", "SEE", "SHE", "SO", "SOME", "TAKE", "THAN", "THAT", "THE", "THEIR", "THEM", "THEN", "THERE", "THESE", "THEY", "THING", "THINK", "THIS", "THOSE", "TIME", "TO", "TWO", "UP", "USE", "VERY", "WANT", "WAY", "WE", "WELL", "WHAT", "WHEN", "WHICH", "WHO", "WILL", "WITH", "WOULD", "YEAR", "YOU", "YOUR",
                "ABLE", "ABOVE", "ACROSS", "ACT", "ACTION", "ACTUAL", "ADD", "AFTER", "AGAIN", "AGAINST", "AGE", "AGO", "AIR", "ALL", "ALLOW", "ALMOST", "ALONE", "ALONG", "ALREADY", "ALTHOUGH", "ALWAYS", "AM", "AMONG", "AMOUNT", "AN", "ANIMAL", "ANOTHER", "ANSWER", "ANY", "ANYTHING", "APPEAR", "ARE", "AREA", "AROUND", "AS", "ASK", "AT", "AWAY", "BABY", "BACK", "BAD", "BALL", "BANK", "BASE", "BE", "BEAR", "BEAT", "BEAUTY", "BECOME", "BED", "BEFORE", "BEGIN", "BEHIND", "BELIEVE", "BELOW", "BEST", "BETTER", "BETWEEN", "BIG", "BIRD", "BIT", "BLACK", "BLOOD", "BLOW", "BLUE", "BOARD", "BOAT", "BODY", "BOOK", "BORN", "BOTH", "BOX", "BOY", "BREAK", "BRING", "BROTHER", "BROWN", "BUILD", "BURN", "BUSINESS", "BUT", "BUY", "BY", "CALL", "CAME", "CAN", "CAR", "CARE", "CARRY", "CASE", "CAT", "CATCH", "CAUSE", "CENT", "CENTER", "CERTAIN", "CHANGE", "CHARGE", "CHECK", "CHILD", "CITY", "CLASS", "CLEAN", "CLEAR", "CLOSE", "CLOTH", "CLOUD", "COAST", "COLD", "COLOR", "COME", "COMPANY", "COMPLETE", "CONSIDER", "CONTAIN", "CONTROL", "COOK", "COOL", "COPY", "CORNER", "COST", "COULD", "COUNT", "COUNTRY", "COURSE", "COVER", "CREATE", "CROSS", "CRY", "CUT", "DARK", "DAY", "DEAD", "DEAL", "DEATH", "DECIDE", "DEEP", "DESCRIBE", "DESIGN", "DETAIL", "DETERMINE", "DEVELOP", "DIE", "DIFFER", "DIFFICULT", "DIRECT", "DISCOVER", "DISTANCE", "DIVIDE", "DO", "DOCTOR", "DOES", "DOG", "DOOR", "DOUBLE", "DOWN", "DRAW", "DREAM", "DRESS", "DRINK", "DRIVE", "DROP", "DRY", "DURING", "EACH", "EAR", "EARLY", "EARTH", "EAST", "EASY", "EAT", "EDGE", "EFFECT", "EGG", "EIGHT", "EITHER", "ELEMENT", "ELSE", "END", "ENERGY", "ENGINE", "ENOUGH", "ENTER", "ENTIRE", "EQUAL", "ESCAPE", "ESPECIALLY", "EVEN", "EVENING", "EVENT", "EVER", "EVERY", "EVERYTHING", "EXACT", "EXAMPLE", "EXCEPT", "EXCITE", "EXERCISE", "EXPECT", "EXPERIENCE", "EXPERIMENT", "EYE", "FACE", "FACT", "FAIL", "FALL", "FAMILY", "FAR", "FARM", "FAST", "FATHER", "FEAR", "FEEL", "FEET", "FEW", "FIELD", "FIGHT", "FIGURE", "FILL", "FINAL", "FIND", "FINE", "FINGER", "FINISH", "FIRE", "FIRST", "FISH", "FIT", "FIVE", "FLAT", "FLOOR", "FLOW", "FLOWER", "FLY", "FOLLOW", "FOOD", "FOOT", "FOR", "FORCE", "FOREST", "FORGET", "FORM", "FORMER", "FORWARD", "FOUND", "FOUR", "FREE", "FRIEND", "FROM", "FRONT", "FRUIT", "FULL", "FUN", "FUNNY", "FURTHER", "FUTURE", "GAME", "GARDEN", "GAS", "GATHER", "GENERAL", "GENTLE", "GET", "GIRL", "GIVE", "GLAD", "GLASS", "GO", "GOLD", "GOOD", "GOT", "GOVERN", "GRASS", "GREAT", "GREEN", "GROUND", "GROUP", "GROW", "GUESS", "GUIDE", "GUN", "HAIR", "HALF", "HAND", "HANG", "HAPPEN", "HAPPY", "HARD", "HAS", "HAT", "HAVE", "HE", "HEAD", "HEAR", "HEART", "HEAT", "HEAVY", "HELD", "HELP", "HER", "HERE", "HIGH", "HILL", "HIM", "HIMSELF", "HIS", "HISTORY", "HIT", "HOLD", "HOLE", "HOME", "HOPE", "HORSE", "HOT", "HOUR", "HOUSE", "HOW", "HUNDRED", "HUNT", "HURRY", "HUSBAND", "ICE", "IDEA", "IF", "IMAGINE", "IMPORTANT", "IN", "INCH", "INCLUDE", "INCREASE", "INDEED", "INDIAN", "INFORMATION", "INSIDE", "INSTEAD", "INTEREST", "INTO", "INVENT", "IRON", "IS", "ISLAND", "IT", "ITS", "JOB", "JOIN", "JOURNEY", "JUDGE", "JUMP", "JUST", "KEEP", "KEY", "KILL", "KIND", "KING", "KNOW", "LADY", "LAKE", "LAND", "LANGUAGE", "LARGE", "LAST", "LATE", "LATER", "LAUGH", "LAW", "LAY", "LEAD", "LEARN", "LEAST", "LEAVE", "LEFT", "LEG", "LENGTH", "LESS", "LET", "LETTER", "LEVEL", "LIE", "LIFE", "LIFT", "LIGHT", "LIKE", "LINE", "LIST", "LISTEN", "LITTLE", "LIVE", "LONG", "LOOK", "LOSE", "LOSS", "LOT", "LOUD", "LOVE", "LOW", "MACHINE", "MADE", "MAIN", "MAJOR", "MAKE", "MAN", "MANY", "MAP", "MARK", "MARKET", "MARRY", "MASS", "MATCH", "MATERIAL", "MATTER", "MAY", "ME", "MEAL", "MEAN", "MEASURE", "MEAT", "MEET", "MEMBER", "MEMORY", "MEN", "MENTION", "METHOD", "MIDDLE", "MIGHT", "MILE", "MILK", "MILLION", "MIND", "MINE", "MINUTE", "MISS", "MIX", "MODEL", "MODERN", "MOMENT", "MONEY", "MONTH", "MOON", "MORE", "MORNING", "MOST", "MOTHER", "MOUNTAIN", "MOUTH", "MOVE", "MUCH", "MUSIC", "MUST", "MY", "NAME", "NATION", "NATURAL", "NEAR", "NECESSARY", "NEED", "NEVER", "NEW", "NEWS", "NEXT", "NICE", "NIGHT", "NINE", "NO", "NORTH", "NOSE", "NOT", "NOTE", "NOTHING", "NOTICE", "NOW", "NUMBER", "OBJECT", "OBSERVE", "OCEAN", "OF", "OFF", "OFFER", "OFFICE", "OFTEN", "OH", "OIL", "OLD", "ON", "ONCE", "ONE", "ONLY", "OPEN", "OPERATE", "OPINION", "OPPORTUNITY", "OR", "ORDER", "ORIGINAL", "OTHER", "OUR", "OUT", "OUTSIDE", "OVER", "OWN", "PAGE", "PAIN", "PAINT", "PAIR", "PAPER", "PARAGRAPH", "PARK", "PART", "PARTICULAR", "PARTY", "PASS", "PAST", "PATH", "PATIENT", "PATTERN", "PAY", "PEACE", "PEN", "PEOPLE", "PER", "PERHAPS", "PERIOD", "PERSON", "PHRASE", "PICK", "PICTURE", "PIECE", "PLACE", "PLAIN", "PLAN", "PLANE", "PLANT", "PLAY", "PLEASE", "PLENTY", "POEM", "POINT", "POOR", "POSITION", "POSSIBLE", "POUND", "POWER", "PRACTICE", "PREPARE", "PRESENT", "PRESS", "PRETTY", "PRICE", "PRINT", "PROBABLE", "PROBLEM", "PROCESS", "PRODUCE", "PRODUCT", "PROGRAM", "PROVE", "PROVIDE", "PUBLIC", "PULL", "PURPOSE", "PUSH", "PUT", "QUESTION", "QUICK", "QUIET", "QUITE", "RACE", "RADIO", "RAIN", "RAISE", "RANGE", "RATE", "RATHER", "REACH", "READ", "READY", "REAL", "REALIZE", "REASON", "RECEIVE", "RECENT", "RECORD", "RED", "REFER", "REGION", "RELATE", "REMAIN", "REMEMBER", "REMOVE", "REPEAT", "REPLY", "REPORT", "REPRESENT", "REQUIRE", "REST", "RESULT", "RETURN", "RICH", "RIDE", "RIGHT", "RING", "RISE", "RIVER", "ROAD", "ROCK", "ROLL", "ROOM", "ROOT", "ROPE", "ROSE", "ROUGH", "ROUND", "ROW", "RULE", "RUN", "SAFE", "SAID", "SAIL", "SALT", "SAME", "SAND", "SAVE", "SAW", "SAY", "SCALE", "SCHOOL", "SCIENCE", "SCORE", "SCRABBLE", "SEA", "SEARCH", "SEASON", "SEAT", "SECOND", "SECRET", "SECTION", "SEE", "SEED", "SEEM", "SELL", "SEND", "SENSE", "SENT", "SENTENCE", "SEPARATE", "SERIOUS", "SERVE", "SET", "SETTLE", "SEVEN", "SEVERAL", "SHALL", "SHAPE", "SHARE", "SHARP", "SHE", "SHEET", "SHELF", "SHELL", "SHINE", "SHIP", "SHIRT", "SHOE", "SHOOT", "SHOP", "SHORE", "SHORT", "SHOT", "SHOULD", "SHOULDER", "SHOUT", "SHOW", "SIDE", "SIGHT", "SIGN", "SILENT", "SIMILAR", "SIMPLE", "SINCE", "SING", "SINGLE", "SIR", "SISTER", "SIT", "SITUATION", "SIX", "SIZE", "SKILL", "SKIN", "SKY", "SLEEP", "SLOW", "SMALL", "SMILE", "SNOW", "SO", "SOCIAL", "SOCIETY", "SOFT", "SOIL", "SOLDIER", "SOLUTION", "SOME", "SOMEONE", "SOMETHING", "SOMETIME", "SON", "SONG", "SOON", "SORRY", "SOUND", "SOUTH", "SPACE", "SPEAK", "SPECIAL", "SPEED", "SPELL", "SPEND", "SPIRIT", "SPORT", "SPOT", "SPREAD", "SPRING", "SQUARE", "STAND", "STAR", "START", "STATE", "STATION", "STAY", "STEADY", "STEAM", "STEEL", "STEP", "STICK", "STILL", "STONE", "STOP", "STORE", "STORY", "STRAIGHT", "STRANGE", "STREAM", "STREET", "STRETCH", "STRING", "STRONG", "STUDENT", "STUDY", "STUFF", "STYLE", "SUBJECT", "SUDDEN", "SUGAR", "SUGGEST", "SUIT", "SUMMER", "SUN", "SUPPLY", "SUPPORT", "SURE", "SURFACE", "SURPRISE", "SWIM", "SYSTEM", "TABLE", "TAIL", "TAKE", "TALK", "TALL", "TEA", "TEACH", "TEAM", "TEAR", "TELEPHONE", "TELL", "TEN", "TENTH", "TERM", "TEST", "THAN", "THANK", "THAT", "THE", "THEIR", "THEM", "THEN", "THERE", "THESE", "THEY", "THICK", "THIN", "THING", "THINK", "THIRD", "THIS", "THOSE", "THOUGH", "THOUGHT", "THOUSAND", "THREAD", "THREE", "THROUGH", "THROW", "THUS", "TIE", "TIGHT", "TILL", "TIME", "TINY", "TIP", "TIRED", "TO", "TODAY", "TOGETHER", "TOMORROW", "TONE", "TOO", "TOOK", "TOOL", "TOOTH", "TOP", "TOTAL", "TOUCH", "TOWARD", "TOWN", "TRACK", "TRADE", "TRAIN", "TRAVEL", "TREE", "TRIANGLE", "TRIP", "TROUBLE", "TRUCK", "TRUE", "TRUST", "TRY", "TUBE", "TURN", "TWELVE", "TWENTY", "TWICE", "TWO", "TYPE", "UNDER", "UNIT", "UNTIL", "UP", "UPON", "US", "USE", "USUAL", "VALUE", "VARIETY", "VERY", "VIEW", "VILLAGE", "VISIT", "VOICE", "VOWEL", "WAIT", "WALK", "WALL", "WANT", "WAR", "WARM", "WAS", "WASH", "WATCH", "WATER", "WAVE", "WAY", "WE", "WEAK", "WEAR", "WEATHER", "WEEK", "WEIGHT", "WELCOME", "WELL", "WENT", "WERE", "WEST", "WET", "WHAT", "WHEEL", "WHEN", "WHERE", "WHETHER", "WHICH", "WHILE", "WHITE", "WHO", "WHOLE", "WHOSE", "WHY", "WIDE", "WIFE", "WILD", "WILL", "WIN", "WIND", "WINDOW", "WING", "WINTER", "WIRE", "WISE", "WISH", "WITH", "WITHIN", "WITHOUT", "WOMAN", "WONDER", "WOOD", "WORD", "WORK", "WORKER", "WORLD", "WORRY", "WORSE", "WORTH", "WOULD", "WRITE", "WRONG", "YARD", "YEAR", "YELLOW", "YES", "YESTERDAY", "YET", "YOU", "YOUNG", "YOUR", "ZERO"
        };

        for (String word : commonEnglishWords) {
            dictionary.add(word.toUpperCase());
        }
        System.out.println("Создан резервный словарь из " + dictionary.size() + " английских слов");
    }

    public static boolean isValidWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }

        // Приводим к верхнему регистру и проверяем
        String normalizedWord = word.trim().toUpperCase();

        // Проверяем, что слово состоит только из букв
        if (!normalizedWord.matches("^[A-Z]+$")) {
            return false;
        }

        // Загружаем словарь, если еще не загружен
        if (dictionary == null) {
            loadDictionary();
        }

        boolean isValid = dictionary.contains(normalizedWord);

        // Для отладки
        if (!isValid && normalizedWord.length() <= 10) {
            System.out.println("Слово не найдено в словаре: " + normalizedWord);
        }

        return isValid;
    }

    public static boolean isValidWordIgnoreCase(String word) {
        return isValidWord(word);
    }

    // Метод для проверки префикса (может быть полезен для поиска слов)
    public static boolean hasWordsWithPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty() || dictionary == null) {
            return false;
        }

        String normalizedPrefix = prefix.toUpperCase();
        for (String word : dictionary) {
            if (word.startsWith(normalizedPrefix)) {
                return true;
            }
        }
        return false;
    }

    // Метод для получения всех слов заданной длины
    public static Set<String> getWordsOfLength(int length) {
        if (dictionary == null) {
            loadDictionary();
        }

        Set<String> result = new HashSet<>();
        for (String word : dictionary) {
            if (word.length() == length) {
                result.add(word);
            }
        }
        return result;
    }

    // Метод для поиска слов по маске (например, "C?T" для CAT, COT и т.д.)
    public static Set<String> findWordsByPattern(String pattern) {
        if (pattern == null || pattern.isEmpty() || dictionary == null) {
            return new HashSet<>();
        }

        String normalizedPattern = pattern.toUpperCase();
        Set<String> result = new HashSet<>();

        for (String word : dictionary) {
            if (word.length() != normalizedPattern.length()) {
                continue;
            }

            boolean matches = true;
            for (int i = 0; i < normalizedPattern.length(); i++) {
                char patternChar = normalizedPattern.charAt(i);
                char wordChar = word.charAt(i);

                if (patternChar != '?' && patternChar != wordChar) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                result.add(word);
            }
        }

        return result;
    }
}