package xyz.zcraft.platform.napcat;

import xyz.zcraft.Config;
import xyz.zcraft.platform.BotPlatformAdapter;
import xyz.zcraft.platform.PlatformGatewayClient;
import xyz.zcraft.platform.PlatformMessageSender;
import xyz.zcraft.util.AccessToken;

import java.net.URI;
import java.util.function.Supplier;

public class NapcatPlatformAdapter implements BotPlatformAdapter {
    private static final long NAPCAT_TOKEN_TTL_SECONDS = 86400L * 365;

    private final Config config;

    public NapcatPlatformAdapter(Config config) {
        this.config = config;
    }

    @Override
    public AccessToken getAccessToken(Config config) {
        return new AccessToken(config.napcatToken(), System.currentTimeMillis(), NAPCAT_TOKEN_TTL_SECONDS);
    }

    @Override
    public String getGatewayEndpoint(AccessToken accessToken) {
        return config.napcatWsEndpoint();
    }

    @Override
    public PlatformMessageSender createMessageSender(Supplier<AccessToken> tokenSupplier) {
        return new NapcatMessageSender(config.napcatHttpEndpoint(), config.napcatToken());
    }

    @Override
    public PlatformGatewayClient createGatewayClient(URI serverUri, Config config, Supplier<AccessToken> tokenSupplier, PlatformMessageSender messageSender) {
        return new NapcatGatewayWebSocketClient(serverUri, messageSender);
    }
}

