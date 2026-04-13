package xyz.zcraft.platform.qq;

import xyz.zcraft.data.FileInfo;
import xyz.zcraft.data.Message;
import xyz.zcraft.platform.PlatformPrivateMessageApi;
import xyz.zcraft.util.AccessToken;
import xyz.zcraft.util.NetworkHelper;

import java.util.function.Supplier;

public class QqPrivateMessageApi implements PlatformPrivateMessageApi {
    private final Supplier<AccessToken> tokenSupplier;

    public QqPrivateMessageApi(Supplier<AccessToken> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    @Override
    public void sendPrivateMessage(String userId, Message message) {
        NetworkHelper.sendPrivateMessage(tokenSupplier.get(), userId, message);
    }

    @Override
    public FileInfo uploadPrivateMedia(String userId, int fileType, String url) {
        return NetworkHelper.uploadPrivateMedia(tokenSupplier.get(), userId, fileType, url);
    }

    @Override
    public FileInfo uploadPrivateMediaBase64(String userId, int fileType, String base64Str) {
        return NetworkHelper.uploadPrivateMediaBase64(tokenSupplier.get(), userId, fileType, base64Str);
    }
}

