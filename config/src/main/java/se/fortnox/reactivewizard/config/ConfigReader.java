package se.fortnox.reactivewizard.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles reading and parsing configuration files in YAML format.
 */
public class ConfigReader {
    private static final Charset      UTF_8           = Charset.forName("UTF-8");
    private static final ObjectMapper mapper          = new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Pattern      ENV_PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z_-]+)\\}\\}");

    /**
     * Create an instance of a configuration from a file path.
     *
     * @param configFile Path to the configuration file
     * @param cls        The class to instantiate with the file
     * @return The instance of the class based on configuration
     */
    public static <T> T fromFile(String configFile, Class<T> cls) {
        try {
            return fromTree(readTree(configFile), cls);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String readFile(String fileName) throws IOException {
        String config = new String(Files.readAllBytes(Paths.get(fileName)), UTF_8);
        return replaceEnvPlaceholders(config);
    }

    /**
     * Replace placeholders like {{HOSTNAME}} with the environment variable named the same.
     * Allows for dynamic configurations that adapt to the environment it is run in.
     */
    private static String replaceEnvPlaceholders(String config) {
        Matcher matcher = ENV_PLACEHOLDER.matcher(config);
        if (!matcher.find()) {
            return config;
        }
        Map<String, String> env          = System.getenv();
        StringBuffer        stringBuffer = new StringBuffer();
        do {
            String value = env.get(matcher.group(1));
            matcher.appendReplacement(stringBuffer, value == null ? "" : value);
        }
        while (matcher.find());
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    public static <T> T fromTree(JsonNode tree, Class<T> cls) {
        String   fieldName = cls.getAnnotation(Config.class).value();
        JsonNode obj       = tree.get(fieldName);

        T cfg;
        try {
            if (obj == null) {
                cfg = cls.newInstance();
            } else {
                ObjectReader reader = mapper.reader(cls);
                cfg = reader.readValue(obj);
            }

            // TODO Disabled until we migrate the validation module
            // ValidatorUtil.validate(cfg);
        } catch (Exception e) {
            throw new RuntimeException("Error reading " + fieldName + " for config " + cls.getName(), e);
        }

        return cfg;
    }

    public static JsonNode readTree(String fileName) {
        try {
            return mapper.readTree(readFile(fileName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
