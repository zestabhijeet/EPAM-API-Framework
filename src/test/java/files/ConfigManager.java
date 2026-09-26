package files;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

public class ConfigManager {
    private static final Properties prop = new Properties();

    public static void load(Path path) {
        Objects.requireNonNull(path, "path");
        try (InputStream in = Files.newInputStream(path)) {
            prop.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load config properties from path: " + path, e);
        }
    }
    public static String getProperty(String key) {
        String value = prop.getProperty(key);
        if (value != null) {
            return value;
        }
        String upperUnderscoreKey = key.replace('.', '_').toUpperCase(Locale.ROOT);
        value = prop.getProperty(upperUnderscoreKey);
        if (value != null) {
            return value;
        }
        String lowerDottedKey = key.replace('_', '.').toLowerCase(Locale.ROOT);
        value = prop.getProperty(lowerDottedKey);
        if (value != null) {
            return value;
        }
        throw new IllegalArgumentException("Missing property: " + key);
    }

    /**
     * Same lookup as {@link #getProperty(String)} (dotted/underscore/upper/lower
     * variants), but returns {@code defaultValue} instead of throwing when the
     * property is absent or blank.
     */
    public static String getProperty(String key, String defaultValue) {
        try {
            String value = getProperty(key);
            return value.isBlank() ? defaultValue : value;
        } catch (IllegalArgumentException missing) {
            return defaultValue;
        }
    }

}
