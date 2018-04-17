package se.fortnox.reactivewizard.util;

import java.util.regex.Pattern;

public class CamelSnakeConverter {

    private static final Pattern camelRegex       = Pattern.compile("([a-z])([A-Z]+)");
    private static final String  snakeReplacement = "$1_$2";

    public static String camelToSnake(String camelString) {
        return camelRegex.matcher(camelString).replaceAll(snakeReplacement).toLowerCase();
    }

    public static String snakeToCamel(String snakeString) {
        String        lowerCaseSnakeString = snakeString.toLowerCase();
        StringBuilder camel                = new StringBuilder();
        boolean       nextUpper            = false;
        for (char c : lowerCaseSnakeString.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else if (nextUpper) {
                camel.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                camel.append(c);
            }
        }
        return camel.toString();
    }

}
