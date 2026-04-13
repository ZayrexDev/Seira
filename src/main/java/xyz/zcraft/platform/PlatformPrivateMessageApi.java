package xyz.zcraft.platform;

import xyz.zcraft.data.FileInfo;
import xyz.zcraft.data.Message;

public interface PlatformPrivateMessageApi {
    void sendPrivateMessage(String userId, Message message);

    default void sendGroupMessage(String groupId, Message message) {
        throw new UnsupportedOperationException("Group message is not supported by this API");
    }

    FileInfo uploadPrivateMedia(String userId, int fileType, String url);

    default FileInfo uploadGroupMedia(String groupId, int fileType, String url) {
        throw new UnsupportedOperationException("Group media upload is not supported by this API");
    }

    FileInfo uploadPrivateMediaBase64(String userId, int fileType, String base64Str);

    default FileInfo uploadGroupMediaBase64(String groupId, int fileType, String base64Str) {
        throw new UnsupportedOperationException("Group media upload is not supported by this API");
    }
}

