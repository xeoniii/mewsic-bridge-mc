package mewsicbridge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("mewsic-bridge");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE;

    public boolean enabled = true;
    public boolean showInPauseMenu = true;
    public boolean showOnHomeScreen = true;
    public boolean showInContainers = true;
    public boolean showInOtherMenus = true;
    public int port = 3012;
    public boolean hasCustomPosition = false;
    public int customX = 0;
    public int customY = 0;

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("mewsic-bridge.json");
    }

    public static synchronized void load() {
        try {
            Path path = getConfigPath();
            File file = path.toFile();
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    INSTANCE = GSON.fromJson(reader, ModConfig.class);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[MewsicBridge] Failed to load config, falling back to defaults", e);
        }
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
        }
        if (INSTANCE.port < 1024 || INSTANCE.port > 65535) {
            INSTANCE.port = 3012;
        }
    }

    public static synchronized void save() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
        }
        if (INSTANCE.port < 1024 || INSTANCE.port > 65535) {
            INSTANCE.port = 3012;
        }
        try {
            Path path = getConfigPath();
            File file = path.toFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[MewsicBridge] Failed to save config", e);
        }
    }
}
