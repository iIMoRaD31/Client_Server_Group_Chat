package com.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Simple properties loader for server runtime configuration.
 */
public final class ConfigLoader {
    private final Properties properties = new Properties();

    public ConfigLoader() {
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                properties.load(is);
            }
        } catch (IOException ignored) {
            // Defaults will be used if the file is missing or unreadable.
        }
    }

    public String getHost() {
        return properties.getProperty("server.host", "0.0.0.0");
    }

    public int getPort() {
        try {
            return Integer.parseInt(properties.getProperty("server.port", "3333"));
        } catch (NumberFormatException e) {
            return 3333;
        }
    }
}
