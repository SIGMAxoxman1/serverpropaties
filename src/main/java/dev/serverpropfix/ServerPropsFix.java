package dev.serverpropfix;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

public class ServerPropsFix extends JavaPlugin {

    private final Logger log = getLogger();

    @Override
    public void onLoad() {
        // Runs BEFORE server loads anything — perfect for patching server.properties
        saveDefaultConfig();
        fixServerProperties();
    }

    @Override
    public void onEnable() {
        log.info("ServerPropsFix active - server.properties was patched before startup.");
    }

    private void fixServerProperties() {
        File serverProps = new File(getServer().getWorldContainer(), "server.properties");

        if (!serverProps.exists()) {
            log.warning("server.properties not found!");
            return;
        }

        // Load all overrides from config.yml
        org.bukkit.configuration.file.FileConfiguration cfg = getConfig();
        org.bukkit.configuration.ConfigurationSection overrides = cfg.getConfigurationSection("overrides");

        if (overrides == null || overrides.getKeys(false).isEmpty()) {
            log.warning("No overrides defined in config.yml!");
            return;
        }

        Map<String, String> toSet = new LinkedHashMap<>();
        for (String key : overrides.getKeys(false)) {
            toSet.put(key, overrides.getString(key, ""));
        }

        try {
            List<String> lines = Files.readAllLines(serverProps.toPath());
            List<String> newLines = new ArrayList<>();
            Set<String> applied = new HashSet<>();

            for (String line : lines) {
                String trimmed = line.trim();
                boolean replaced = false;

                for (String key : toSet.keySet()) {
                    if (trimmed.startsWith(key + "=")) {
                        newLines.add(key + "=" + toSet.get(key));
                        applied.add(key);
                        log.info("Fixed: " + key + "=" + toSet.get(key));
                        replaced = true;
                        break;
                    }
                }

                if (!replaced) newLines.add(line);
            }

            // Add any keys that didn't exist in the file
            for (String key : toSet.keySet()) {
                if (!applied.contains(key)) {
                    newLines.add(key + "=" + toSet.get(key));
                    log.info("Added: " + key + "=" + toSet.get(key));
                }
            }

            Files.write(serverProps.toPath(), newLines);
            log.info("server.properties patched successfully! (" + toSet.size() + " properties)");

        } catch (IOException e) {
            log.severe("Failed to patch server.properties: " + e.getMessage());
        }
    }
}
