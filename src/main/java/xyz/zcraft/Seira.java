package xyz.zcraft;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.config.AppConfig;
import xyz.zcraft.config.ConfigLoader;
import xyz.zcraft.platform.BotPlatformAdapter;
import xyz.zcraft.platform.BotPlatformAdapters;
import xyz.zcraft.platform.PlatformGatewayClient;
import xyz.zcraft.platform.PlatformMessageSender;
import xyz.zcraft.util.AccessToken;
import xyz.zcraft.util.ThreadHelper;

import java.io.IOException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class Seira {
    private static final Logger LOG = LogManager.getLogger(Seira.class);
    private static final Timer timer = new Timer("access-token-renewal", true);
    @Getter
    private static AppConfig config;
    private static AccessToken accessToken;
    private static BotPlatformAdapter platformAdapter;

    static void main() {
        LOG.info("Loading config");

        if(!ConfigLoader.configExists()) {
            LOG.warn("Config file does not exist, copying default config. Please check your config file.");
            try {
                ConfigLoader.copyDefaultConfig();
            } catch (IOException e) {
                LOG.error("Failed to copy default config", e);
            }

            System.exit(0);
        }

        try {
            config = ConfigLoader.loadConfig();
        } catch (Exception e) {
            LOG.error("Invalid configuration! Please check your .env file.");
            System.exit(1);
            return;
        }

        platformAdapter = BotPlatformAdapters.create(config);
        LOG.info("Selected platform adapter: {}", config.seira().platform());

        LOG.info("Getting access token");
        renewAccessToken();

        PlatformMessageSender messageSender = platformAdapter.createMessageSender(() -> accessToken);

        Runtime.getRuntime().addShutdownHook(new Thread(ThreadHelper::close));

        while (true) {
            try {
                LOG.info("Getting wss endpoint");
                String wssEndpoint = platformAdapter.getGatewayEndpoint(accessToken);
                LOG.info("Endpoint: {}", wssEndpoint);

                PlatformGatewayClient client = platformAdapter.createGatewayClient(
                        URI.create(wssEndpoint),
                        config,
                        () -> accessToken,
                        messageSender
                );
                client.connectBlocking();
                LOG.info("Gateway session started");
                while (client.isOpen()) {
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                LOG.error("Gateway loop failed", e);
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public static void renewAccessToken() {
        if (accessToken == null || accessToken.isExpired()) {
            accessToken = platformAdapter.getAccessToken(config);
            LOG.info("Token renewed, expire in {}", accessToken.expiresIn());
            long renewDelayMs = Math.max(5, accessToken.expiresIn() - 60) * 1000L;
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    renewAccessToken();
                }
            }, renewDelayMs);
        }
    }
}
