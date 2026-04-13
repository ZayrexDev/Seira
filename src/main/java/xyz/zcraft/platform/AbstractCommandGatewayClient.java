package xyz.zcraft.platform;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import xyz.zcraft.api.APIHelper;
import xyz.zcraft.data.FileInfo;
import xyz.zcraft.data.Message;
import xyz.zcraft.data.PendingMessage;

import java.net.URI;
import java.util.Arrays;

public abstract class AbstractCommandGatewayClient extends WebSocketClient implements PlatformGatewayClient {
    private static final Logger LOG = LogManager.getLogger(AbstractCommandGatewayClient.class);
    private static final String PREFIX = "/";

    private final PlatformMessageSender messageSender;

    protected AbstractCommandGatewayClient(URI serverUri, PlatformMessageSender messageSender) {
        super(serverUri);
        this.messageSender = messageSender;
    }

    protected void onPrivateMessageReceived(String userId, String messageId, String rawContent) {
        handleMessageReceived(userId, messageId, rawContent, false);
    }

    protected void onGroupMessageReceived(String groupId, String messageId, String rawContent) {
        handleMessageReceived(groupId, messageId, rawContent, true);
    }

    private void handleMessageReceived(String targetId, String messageId, String rawContent, boolean groupMessage) {
        try {
            PendingMessage pendingMsg = route(rawContent);
            if (pendingMsg == null) {
                return;
            }

            Message message = new Message();
            message.setContent(pendingMsg.getContent());
            message.setMsgType(pendingMsg.getMsgType());
            message.setMsgId(messageId);

            if (pendingMsg.getFileUrl() != null) {
                FileInfo fileInfo = groupMessage
                        ? messageSender.uploadGroupMedia(targetId, pendingMsg.getFileType(), pendingMsg.getFileUrl())
                        : messageSender.uploadPrivateMedia(targetId, pendingMsg.getFileType(), pendingMsg.getFileUrl());
                if (fileInfo == null) {
                    LOG.error("Failed to upload media for message {}", messageId);
                    return;
                }
                LOG.info("Media uploaded for message {}", messageId);
                message.setMedia(fileInfo);
            } else if (pendingMsg.getFileBase64() != null) {
                FileInfo fileInfo = groupMessage
                        ? messageSender.uploadGroupMediaBase64(targetId, pendingMsg.getFileType(), pendingMsg.getFileBase64())
                        : messageSender.uploadPrivateMediaBase64(targetId, pendingMsg.getFileType(), pendingMsg.getFileBase64());
                if (fileInfo == null) {
                    LOG.error("Failed to upload base64 media for message {}", messageId);
                    return;
                }
                LOG.info("Base64 media uploaded for message {}", messageId);
                message.setMedia(fileInfo);
            }

            if (groupMessage) {
                messageSender.sendGroupMessage(targetId, message);
            } else {
                messageSender.sendPrivateMessage(targetId, message);
            }
        } catch (Exception e) {
            Message message = new Message();
            message.setContent("处理指令时发生错误，请稍后再试。");
            message.setMsgType(0);
            message.setMsgId(messageId);
            if (groupMessage) {
                messageSender.sendGroupMessage(targetId, message);
            } else {
                messageSender.sendPrivateMessage(targetId, message);
            }
            LOG.error("Failed to process inbound message {}", messageId, e);
        }
    }

    protected PendingMessage route(String rawContent) {
        if (rawContent == null || !rawContent.startsWith(PREFIX)) {
            return null;
        }

        String body = rawContent.substring(PREFIX.length()).trim();
        if (body.isEmpty()) {
            return PendingMessage.ofString("请输入指令。使用/help获取帮助。");
        }

        String[] parts = body.split("\\s+");
        String command = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        switch (command) {
            case "bo", "top" -> {
                if (args.length == 2) {
                    int n = Integer.parseInt(args[0]);
                    int id = Integer.parseInt(args[1]);
                    return PendingMessage.ofImageBase64(APIHelper.getBoN(n, id));
                } else if (args.length == 1) {
                    // TODO implement this variant in command service.
                } else {
                    return PendingMessage.ofString("用法：/bo <个数> [玩家ID]");
                }
            }
            case "daily" -> {
                return PendingMessage.ofString(APIHelper.getDaily());
            }
            case "mp" -> {
                return PendingMessage.ofString(APIHelper.getMultiplayerRooms());
            }
            case "rs" -> {
                if (args.length == 2) {
                    int n = Integer.parseInt(args[0]);
                    int id = Integer.parseInt(args[1]);
                    return PendingMessage.ofImageBase64(APIHelper.getRecent(n, id));
                } else if (args.length == 1) {
                    // TODO implement this variant in command service.
                } else {
                    return PendingMessage.ofString("用法：/bo <个数> [玩家ID]");
                }
            }
            case "bm" -> {
                if (args.length == 1) {
                    int id = Integer.parseInt(args[0]);
                    return PendingMessage.ofImageBase64(APIHelper.getBeatmap(id));
                } else {
                    return PendingMessage.ofString("用法：/bm <铺面ID>");
                }
            }
            case "c" -> {
                if (args.length == 2) {
                    int bm = Integer.parseInt(args[0]);
                    String uids = args[1];
                    return PendingMessage.ofImageBase64(APIHelper.getGroupLeaderboard(bm, uids.split(",")));
                } else if (args.length == 1) {
                    // TODO implement this variant in command service.
                } else {
                    return PendingMessage.ofString("用法：/c <铺面ID> [玩家ID,[玩家ID...]]");
                }
            }
            case "status" -> {
                return PendingMessage.ofString("服务器状态：正常");
            }
            default -> {
                return PendingMessage.ofString("未知指令。使用/help获取帮助。");
            }
        }

        return PendingMessage.ofString("未知指令。使用/help获取帮助。");
    }
}

