package com.plr.aduaja.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class DatabaseRecoveryEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DatabaseRecoveryEnvironmentPostProcessor.class);
    private static final String DOTENV_PROPERTY_SOURCE = "aduaja-dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        loadDotenv(environment);
        String dbType = environment.getProperty("DB_TYPE", "postgres");
        if ("h2".equalsIgnoreCase(dbType)) {
            DatabaseStartupRecovery.prepareDatabase();
        }
    }

    private void loadDotenv(ConfigurableEnvironment environment) {
        Path dotenvPath = findDotenvPath();
        if (dotenvPath == null) {
            log.warn(".env tidak ditemukan. Pastikan file .env ada di folder project.");
            return;
        }

        log.info("Memuat .env dari: {}", dotenvPath.toAbsolutePath());

        try {
            Map<String, Object> props = new HashMap<>();
            try (Stream<String> lines = Files.lines(dotenvPath)) {
                lines.map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .filter(line -> line.contains("="))
                        .forEach(line -> {
                            int eq = line.indexOf('=');
                            String key = line.substring(0, eq).trim();
                            String value = line.substring(eq + 1).trim();
                            if (!key.isEmpty()) {
                                props.put(key, value);
                            }
                        });
            }
            if (!props.isEmpty()) {
                MutablePropertySources sources = environment.getPropertySources();
                MapPropertySource source = new MapPropertySource(DOTENV_PROPERTY_SOURCE, props);
                if (sources.contains(DOTENV_PROPERTY_SOURCE)) {
                    sources.replace(DOTENV_PROPERTY_SOURCE, source);
                } else {
                    sources.addFirst(source);
                }
                log.info("Memuat {} variabel dari .env", props.size());
            }
        } catch (IOException e) {
            log.error("Gagal membaca .env: {}", e.getMessage());
        }
    }

    private Path findDotenvPath() {
        Path cwd = Path.of(".env").toAbsolutePath();
        if (Files.exists(cwd)) return cwd;

        try {
            Path jarDir = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            if (jarDir != null) {
                Path nextToJar = jarDir.resolve(".env");
                if (Files.exists(nextToJar)) return nextToJar;
            }
        } catch (Exception ignored) {}

        return null;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
