package xyz.zcraft.platform.napcat;

import xyz.zcraft.config.AppConfig;
import xyz.zcraft.platform.BotPlatformAdapter;
import xyz.zcraft.platform.PlatformGatewayClient;
import xyz.zcraft.platform.PlatformMessageSender;
import xyz.zcraft.util.AccessToken;

import java.net.URI;
import java.util.function.Supplier;

public class NapcatPlatformAdapter implements BotPlatformAdapter {
    private static final long NAPCAT_TOKEN_TTL_SECONDS = 86400L * 365;

    private final AppConfig config;

    public NapcatPlatformAdapter(AppConfig config) {
        this.config = config;
    }

    @Override
    public AccessToken getAccessToken(AppConfig config) {
        return new AccessToken(config.platforms().napcat().token(), System.currentTimeMillis(), NAPCAT_TOKEN_TTL_SECONDS);
    }

    @Override
    public String getGatewayEndpoint(AccessToken accessToken) {
        return config.platforms().napcat().wsEndpoint();
    }

    @Override
    public PlatformMessageSender createMessageSender(Supplier<AccessToken> tokenSupplier) {
        return new NapcatMessageSender(config.platforms().napcat().httpEndpoint(), config.platforms().napcat().token());
    }

    @Override
    public PlatformGatewayClient createGatewayClient(URI serverUri, AppConfig config, Supplier<AccessToken> tokenSupplier, PlatformMessageSender messageSender) {
        return new NapcatGatewayWebSocketClient(serverUri, messageSender, this.config.platforms().napcat().token());
    }
}

