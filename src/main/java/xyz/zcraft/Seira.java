package xyz.zcraft;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.bot.GatewayWebSocketClient;
import xyz.zcraft.bot.MessageSender;
import xyz.zcraft.util.AccessToken;
import xyz.zcraft.util.NetworkHelper;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class Seira {
    private static final Logger LOG = LogManager.getLogger(Seira.class);
    @Getter
    private static Config config;
    private static AccessToken accessToken;
    private static final Timer timer = new Timer("access-token-renewal", true);

    static void main() {
        LOG.info("Loading .env ...");
        final Dotenv env = Dotenv.load();
        config = Config.fromEnv(env);

        LOG.info("Getting access token");
        renewAccessToken();

        MessageSender messageSender = new MessageSender(() -> accessToken);

        while (true) {
            try {
                LOG.info("Getting wss endpoint");
                String wssEndpoint = NetworkHelper.getWSSEndpoint(accessToken);
                LOG.info("Endpoint: {}", wssEndpoint);

                GatewayWebSocketClient client = new GatewayWebSocketClient(
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
            accessToken = NetworkHelper.getAccessToken(config);
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
