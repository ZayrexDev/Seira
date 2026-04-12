package xyz.zcraft.platform.qq;

import xyz.zcraft.Config;
import xyz.zcraft.platform.BotPlatformAdapter;
import xyz.zcraft.platform.PlatformGatewayClient;
import xyz.zcraft.platform.PlatformMessageSender;
import xyz.zcraft.util.AccessToken;
import xyz.zcraft.util.NetworkHelper;

import java.net.URI;
import java.util.function.Supplier;

public class QqPlatformAdapter implements BotPlatformAdapter {
    @Override
    public AccessToken getAccessToken(Config config) {
        return NetworkHelper.getAccessToken(config);
    }

    @Override
    public String getGatewayEndpoint(AccessToken accessToken) {
        return NetworkHelper.getWSSEndpoint(accessToken);
    }

    @Override
    public PlatformMessageSender createMessageSender(Supplier<AccessToken> tokenSupplier) {
        return new MessageSender(tokenSupplier);
    }

    @Override
    public PlatformGatewayClient createGatewayClient(
            URI serverUri,
            Config config,
            Supplier<AccessToken> tokenSupplier,
            PlatformMessageSender messageSender
    ) {
        return new GatewayWebSocketClient(serverUri, config, tokenSupplier, messageSender);
    }
}

