package xyz.zcraft.platform;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import xyz.zcraft.Seira;
import xyz.zcraft.api.APIHelper;
import xyz.zcraft.binding.UserBindingStore;
import xyz.zcraft.data.FileInfo;
import xyz.zcraft.data.Message;
import xyz.zcraft.data.PendingMessage;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractCommandGatewayClient extends WebSocketClient implements PlatformGatewayClient {
    private static final Logger LOG = LogManager.getLogger(AbstractCommandGatewayClient.class);
    private static final String PREFIX = "/";

    private final PlatformMessageSender messageSender;

    protected AbstractCommandGatewayClient(URI serverUri, PlatformMessageSender messageSender) {
        super(serverUri);
        this.messageSender = messageSender;
    }

    protected void onPrivateMessageReceived(String userId, String messageId, String rawContent) {
        handleMessageReceived(userId, null, userId, messageId, rawContent, false);
    }

    protected void onGroupMessageReceived(String groupId, String senderUserId, String messageId, String rawContent) {
        handleMessageReceived(groupId, groupId, senderUserId, messageId, rawContent, true);
    }

    private void handleMessageReceived(String targetId, String groupId, String senderUserId, String messageId, String rawContent, boolean groupMessage) {
        try {
            String platform = Seira.getConfig().platform();
            if (groupMessage && groupId != null && !groupId.isBlank() && senderUserId != null && !senderUserId.isBlank()) {
                UserBindingStore.upsertGroupMember(platform, groupId, senderUserId);
            }

            PendingMessage pendingMsg = route(rawContent, senderUserId, groupId);
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

    protected PendingMessage route(String rawContent, String senderUserId, String groupId) {
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
        String platform = Seira.getConfig().platform();

        switch (command) {
            case "bind" -> {
                if (senderUserId == null || senderUserId.isBlank()) {
                    return PendingMessage.ofString("无法识别你的用户ID，暂时无法绑定。请稍后重试。");
                }
                if (args.length != 1) {
                    return PendingMessage.ofString("用法：/bind <玩家ID>");
                }
                Integer uid = parsePositiveInt(args[0]);
                if (uid == null) {
                    return PendingMessage.ofString("玩家ID必须是正整数。用法：/bind <玩家ID>");
                }
                UserBindingStore.bind(platform, senderUserId, uid);
                return PendingMessage.ofString("绑定成功，已绑定到玩家ID: " + uid);
            }
            case "bo", "top" -> {
                if (args.length == 2) {
                    Integer n = parsePositiveInt(args[0]);
                    Integer id = parsePositiveInt(args[1]);
                    if (n == null || id == null) {
                        return PendingMessage.ofString("用法：/bo <个数> [玩家ID]");
                    }
                    return PendingMessage.ofImageBase64(APIHelper.getBoN(n, id));
                } else if (args.length == 1) {
                    Integer n = parsePositiveInt(args[0]);
                    if (n == null) {
                        return PendingMessage.ofString("用法：/bo <个数> [玩家ID]");
                    }
                    Integer uid = resolveBoundUid(platform, senderUserId);
                    if (uid == null) {
                        return PendingMessage.ofString("你还没有绑定玩家ID，请先使用 /bind <玩家ID>");
                    }
                    return PendingMessage.ofImageBase64(APIHelper.getBoN(n, uid));
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
                    Integer n = parsePositiveInt(args[0]);
                    Integer id = parsePositiveInt(args[1]);
                    if (n == null || id == null) {
                        return PendingMessage.ofString("用法：/rs <个数> [玩家ID]");
                    }
                    return PendingMessage.ofImageBase64(APIHelper.getRecent(n, id));
                } else if (args.length == 1) {
                    Integer n = parsePositiveInt(args[0]);
                    if (n == null) {
                        return PendingMessage.ofString("用法：/rs <个数> [玩家ID]");
                    }
                    Integer uid = resolveBoundUid(platform, senderUserId);
                    if (uid == null) {
                        return PendingMessage.ofString("你还没有绑定玩家ID，请先使用 /bind <玩家ID>");
                    }
                    return PendingMessage.ofImageBase64(APIHelper.getRecent(n, uid));
                } else {
                    return PendingMessage.ofString("用法：/rs <个数> [玩家ID]");
                }
            }
            case "m" -> {
                if (args.length == 2) {
                    Integer id = parsePositiveInt(args[0]);
                    String mod = args[1];
                    if (id == null) {
                        return PendingMessage.ofString("用法：/m <铺面ID> [+Mod]");
                    }
                    return PendingMessage.ofImageBase64(APIHelper.getBeatmap(id, mod));
                } else if (args.length == 1) {
                    Integer id = parsePositiveInt(args[0]);
                    if (id == null) {
                        return PendingMessage.ofString("用法：/m <铺面ID> [+Mod]");
                    }
                    return PendingMessage.ofImageBase64(APIHelper.getBeatmap(id, null));
                } else {
                    return PendingMessage.ofString("用法：/m <铺面ID> [+Mod]");
                }
            }
            case "lb" -> {
                if (args.length == 0) {
                    if (groupId != null && !groupId.isBlank()) {
                        List<Integer> groupBoundUids = UserBindingStore.findBoundUidsByGroup(platform, groupId);
                        if (groupBoundUids.isEmpty()) {
                            return PendingMessage.ofString("本群还没有已绑定的玩家，请先使用 /bind <玩家ID>");
                        }
                        String[] uidArray = groupBoundUids.stream()
                                .map(String::valueOf)
                                .toArray(String[]::new);
                        return PendingMessage.ofImageBase64(APIHelper.getLeaderboard(uidArray));
                    } else {
                        return PendingMessage.ofString("本群还没有已绑定的玩家，请先使用 /bind <玩家ID>");
                    }
                } else if (args.length == 1) {
                    Integer bm = parsePositiveInt(args[0]);
                    if (bm == null) {
                        return PendingMessage.ofString("用法：/lb [铺面ID]");
                    }
                    if (groupId != null && !groupId.isBlank()) {
                        List<Integer> groupBoundUids = UserBindingStore.findBoundUidsByGroup(platform, groupId);
                        if (groupBoundUids.isEmpty()) {
                            return PendingMessage.ofString("本群还没有已绑定的玩家，请先使用 /bind <玩家ID>");
                        }
                        String[] uidArray = groupBoundUids.stream()
                                .map(String::valueOf)
                                .toArray(String[]::new);
                        return PendingMessage.ofImageBase64(APIHelper.getGroupLeaderboard(bm, uidArray));
                    } else {
                        return PendingMessage.ofString("本群还没有已绑定的玩家，请先使用 /bind <玩家ID>");
                    }
                } else {
                    return PendingMessage.ofString("用法：/lb [铺面ID]");
                }
            }
            case "status" -> {
                return PendingMessage.ofString("服务器状态：正常");
            }
            case "help" -> {
                return PendingMessage.ofString("""
                        可用指令：
                        /bind <玩家ID> - 绑定你的玩家ID
                        /bo <个数> [玩家ID] - 获取BoN图谱
                        /rs <个数> [玩家ID] - 获取最近成绩图谱
                        /m <铺面ID> - 获取铺面图谱
                        /lb [铺面ID] - 获取排行榜图谱（可选铺面ID）
                        /daily - 获取每日挑战
                        /mp - 获取多人房间列表
                        /status - 获取服务器状态
                        /help - 显示此帮助信息
                        """);
            }
            default -> {
                return PendingMessage.ofString("未知指令。使用/help获取帮助。");
            }
        }

    }

    private Integer resolveBoundUid(String platform, String senderUserId) {
        if (senderUserId == null || senderUserId.isBlank()) {
            return null;
        }
        return UserBindingStore.findBoundUid(platform, senderUserId);
    }

    private Integer parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

