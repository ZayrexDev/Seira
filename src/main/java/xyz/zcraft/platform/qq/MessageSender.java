package xyz.zcraft.platform.qq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.data.FileInfo;
import xyz.zcraft.data.Message;
import xyz.zcraft.platform.PlatformMessageSender;
import xyz.zcraft.util.AccessToken;
import xyz.zcraft.util.NetworkHelper;

import java.util.function.Supplier;

public class MessageSender implements PlatformMessageSender {
    private static final Logger LOG = LogManager.getLogger(MessageSender.class);

    private final Supplier<AccessToken> tokenSupplier;

    public MessageSender(Supplier<AccessToken> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    public void sendPrivateMessage(String openId, Message message) {
        try {
            NetworkHelper.sendPrivateMessage(
                    tokenSupplier.get(),
                    openId,
                    message
            );
        } catch (RuntimeException e) {
            LOG.error("Failed to send message to private {}", openId, e);
        }
    }

    public FileInfo uploadPrivateMedia(String openId, int fileType, String url) {
        try {
            return NetworkHelper.uploadPrivateMedia(
                    tokenSupplier.get(),
                    openId,
                    fileType,
                    url
            );
        } catch (RuntimeException e) {
            LOG.error("Failed to upload private media {}", openId, e);
            return null;
        }
    }

    public FileInfo uploadPrivateMediaBase64(String openId, int fileType, String base64Str) {
        try {
            return NetworkHelper.uploadPrivateMediaBase64(
                    tokenSupplier.get(),
                    openId,
                    fileType,
                    base64Str
            );
        } catch (RuntimeException e) {
            LOG.error("Failed to upload private media {}", openId, e);
            return null;
        }
    }
}
