package se.fortnox.reactivewizard.util;

import java.util.regex.Pattern;

/**
 * Utility to convert between snake_case and camelCase.
 */
public class CamelSnakeConverter {

    private static final Pattern camelRegex       = Pattern.compile("([a-z])([A-Z]+)");
    private static final String  snakeReplacement = "$1_$2";

    /**
     * Converts from camelCasedStrings to snake_cased_strings.
     *
     * @param camelString String in camelCase format
     * @return Input formatted as snake_case format
     */
    public static String camelToSnake(String camelString) {
        return camelRegex.matcher(camelString).replaceAll(snakeReplacement).toLowerCase();
    }

    /**
     * Converts from snake_cased_strings to camelCasedStrings.
     *
     * @param snakeString String in snake_case format
     * @return Input formatted as camelCase format
     */
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
