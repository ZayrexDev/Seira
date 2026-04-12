package xyz.zcraft.platform;

public interface PlatformGatewayClient {
    boolean connectBlocking() throws InterruptedException;

    boolean isOpen();
}
