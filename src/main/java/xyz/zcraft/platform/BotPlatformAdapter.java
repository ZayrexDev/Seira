package xyz.zcraft.platform;

import xyz.zcraft.Config;
import xyz.zcraft.util.AccessToken;

import java.net.URI;
import java.util.function.Supplier;

public interface BotPlatformAdapter {
    AccessToken getAccessToken(Config config);

    String getGatewayEndpoint(AccessToken accessToken);

    PlatformMessageSender createMessageSender(Supplier<AccessToken> tokenSupplier);

    PlatformGatewayClient createGatewayClient(
            URI serverUri,
            Config config,
            Supplier<AccessToken> tokenSupplier,
            PlatformMessageSender messageSender
    );
}

