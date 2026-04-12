package xyz.zcraft.platform;

import xyz.zcraft.data.FileInfo;
import xyz.zcraft.data.Message;

public interface PlatformMessageSender {
    void sendPrivateMessage(String userId, Message message);

    FileInfo uploadPrivateMedia(String userId, int fileType, String url);

    FileInfo uploadPrivateMediaBase64(String userId, int fileType, String base64Str);
}

