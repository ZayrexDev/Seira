package xyz.zcraft.data;

import lombok.Data;

@Data
public class PendingMessage {
    private String content;
    private int msgType;
    private String fileUrl = null;
    private String fileBase64 = null;
    private int fileType = -1;

    public static PendingMessage ofString(String content) {
        final PendingMessage message = new PendingMessage();
        message.content = content;
        message.msgType = 0;
        return message;
    }

    public static PendingMessage ofImageBase64(String imageBase64) {
        final PendingMessage message = new PendingMessage();
        message.fileType = 1;
        message.msgType = 7;
        message.fileBase64 = imageBase64;
        return message;
    }
}
