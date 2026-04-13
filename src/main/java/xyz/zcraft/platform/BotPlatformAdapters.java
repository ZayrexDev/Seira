package xyz.zcraft.platform;

import xyz.zcraft.Config;
import xyz.zcraft.platform.napcat.NapcatPlatformAdapter;
import xyz.zcraft.platform.qq.QqPlatformAdapter;

public final class BotPlatformAdapters {
    private BotPlatformAdapters() {
    }

    public static BotPlatformAdapter create(Config config) {
        return switch (config.platform().toLowerCase()) {
            case "napcat" -> new NapcatPlatformAdapter(config);
            case "qq" -> new QqPlatformAdapter();
            default -> throw new IllegalStateException("Unsupported platform: " + config.platform());
        };
    }
}

