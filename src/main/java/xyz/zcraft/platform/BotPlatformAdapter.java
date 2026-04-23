package xyz.zcraft.platform;

import xyz.zcraft.config.AppConfig;
import xyz.zcraft.util.AccessToken;

import java.net.URI;
import java.util.function.Supplier;

public interface BotPlatformAdapter {
    AccessToken getAccessToken(AppConfig config);

    String getGatewayEndpoint(AccessToken accessToken);

    PlatformMessageSender createMessageSender(Supplier<AccessToken> tokenSupplier);

    PlatformGatewayClient createGatewayClient(
            URI serverUri,
            AppConfig config,
            Supplier<AccessToken> tokenSupplier,
            PlatformMessageSender messageSender
    );
}

