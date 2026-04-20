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
import xyz.zcraft.util.ThreadHelper;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractCommandGatewayClient extends WebSocketClient implements PlatformGatewayClient {
    private static final Logger LOG = LogManager.getLogger(AbstractCommandGatewayClient.class);
    private static final String PREFIX = "/";
    private static final ApiRequestStats API_REQUEST_STATS = new ApiRequestStats();

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
        LOG.info("Received {} message {} from {}: {}", groupMessage ? "group" : "private", messageId, senderUserId, rawContent);
        AtomicInteger messageSeqCounter = new AtomicInteger(1);
        try {
            String platform = Seira.getConfig().platform();
            if (groupMessage && groupId != null && !groupId.isBlank() && senderUserId != null && !senderUserId.isBlank()) {
                UserBindingStore.upsertGroupMember(platform, groupId, senderUserId);
            }

            RouteDecision routeDecision = route(rawContent, senderUserId, groupId);
            if (routeDecision == null) {
                return;
            }

            sendOutboundMessage(targetId, messageId, groupMessage, routeDecision.initialMessage(), messageSeqCounter);

            ApiTask apiTask = routeDecision.apiTask();
            if (apiTask != null) {
                ThreadHelper.run(() -> processApiTask(targetId, messageId, groupMessage, apiTask, messageSeqCounter));
            }
        } catch (Exception e) {
            sendOutboundMessage(targetId, messageId, groupMessage, PendingMessage.ofString("处理指令时发生错误，请稍后再试。"), messageSeqCounter);
            LOG.error("Failed to process inbound message {}", messageId, e);
        }
    }

    protected RouteDecision route(String rawContent, String senderUserId, String groupId) {
        if (rawContent == null || !rawContent.startsWith(PREFIX)) {
            return null;
        }

        String body = rawContent.substring(PREFIX.length()).trim();
        if (body.isEmpty()) {
            return RouteDecision.sync(PendingMessage.ofString("请输入指令。使用/help获取帮助。"));
        }

        String[] parts = body.split("\\s+");
        String command = parts[0].toLowerCase();
        String query = body.substring(command.length()).trim();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        String platform = Seira.getConfig().platform();

        switch (command) {
            case "bind" -> {
                if (senderUserId == null || senderUserId.isBlank()) {
                    return RouteDecision.sync(PendingMessage.ofString("无法识别你的用户ID，暂时无法绑定。请稍后重试。"));
                }
                if (args.length != 1) {
                    return RouteDecision.sync(PendingMessage.ofString("用法：/bind <玩家ID>"));
                }
                Integer uid = parsePositiveInt(args[0]);
                if (uid == null) {
                    return RouteDecision.sync(PendingMessage.ofString("玩家ID必须是正整数。用法：/bind <玩家ID>"));
                }
                UserBindingStore.bind(platform, senderUserId, uid);
                return RouteDecision.sync(PendingMessage.ofString("绑定成功，已绑定到玩家ID: " + uid));
            }
            case "unbind" -> {
                if (senderUserId == null || senderUserId.isBlank()) {
                    return RouteDecision.sync(PendingMessage.ofString("无法识别你的用户ID，暂时无法解绑。请稍后重试。"));
                }
                if (args.length != 0) {
                    return RouteDecision.sync(PendingMessage.ofString("用法：/unbind"));
                }
                boolean removed = UserBindingStore.unbind(platform, senderUserId);
                return RouteDecision.sync(PendingMessage.ofString(removed
                        ? "解绑成功。"
                        : "你当前还没有绑定玩家ID，无需解绑。"));
            }
            case "bo", "top" -> {
                if (args.length == 2) {
                    Integer n = parsePositiveInt(args[0]);
                    Integer id = parsePositiveInt(args[1]);
                    if (n == null || id == null) {
                        return RouteDecision.sync(PendingMessage.ofString("用法：/bo <个数> [玩家ID]"));
                    }
                    return queueApiRequest("bo", () -> PendingMessage.ofImageBase64(APIHelper.getBoN(n, id)));
                } else if (args.length == 1) {
                    Integer n = parsePositiveInt(args[0]);
                    if (n == null) {
                        return RouteDecision.sync(PendingMessage.ofString("用法：/bo <个数> [玩家ID]"));
                    }
                    Integer uid = resolveBoundUid(platform, senderUserId);
                    if (uid == null) {
                        return RouteDecision.sync(PendingMessage.ofString("你还没有绑定玩家ID，请先使用 /bind <玩家ID>"));
                    }
                    return queueApiRequest("bo", () -> PendingMessage.ofImageBase64(APIHelper.getBoN(n, uid)));
                } else {
                    return RouteDecision.sync(PendingMessage.ofString("用法：/bo <个数> [玩家ID]"));
                }
            }
            case "daily" -> {
                return queueApiRequest("daily", () -> PendingMessage.ofString(APIHelper.getDaily()));
            }
            case "mp" -> {
                return queueApiRequest("mp", () -> PendingMessage.ofString(APIHelper.getMultiplayerRooms()));
            }
            case "rs" -> {
                if (args.length == 2) {
                    Integer n = parsePositiveInt(args[0]);
                    Integer id = parsePositiveInt(args[1]);
                    if (n == null || id == null) {
                        return RouteDecision.sync(PendingMessage.ofString("用法：/rs <个数> [玩家ID]"));
                    }
                    return queueApiRequest("rs", () -> PendingMessage.ofImageBase64(APIHelper.getRecent(n, id)));
                } else if (args.length == 1) {
                    Integer n = parsePositiveInt(args[0]);
                    if (n == null) {
                        return RouteDecision.sync(PendingMessage.ofString("用法：/rs <个数> [玩家ID]"));
                    }
                    Integer uid = resolveBoundUid(platform, senderUserId);
                    if (uid == null) {
                        return RouteDecision.sync(PendingMessage.ofString("你还没有绑定玩家ID，请先使用 /bind <玩家ID>"));
                    }
                    return queueApiRequest("rs", () -> PendingMessage.ofImageBase64(APIHelper.getRecent(n, uid)));
                } else {
                    return RouteDecision.sync(PendingMessage.ofString("用法：/rs <个数> [玩家ID]"));
                }
            }
            case "m" -> {
                if (args.length == 2) {
                    Integer id = parsePositiveInt(args[0]);
                    String mod = args[1];
                    if (id == null) {
                        return RouteDecision.sync(PendingMessage.ofString("用法：/m <铺面ID> [Mod]"));
                    }
                    return queueApiRequest("m", () -> PendingMessage.ofImageBase64(APIHelper.getBeatmap(id, mod)));
                } else if (args.length == 1) {
                    Integer id = parsePositiveInt(args[0]);
                    if (id == null) {
                        return RouteDecision.sync(PendingMessage.ofString("用法：/m <铺面ID> [Mod]"));
                    }
                    return queueApiRequest("m", () -> PendingMessage.ofImageBase64(APIHelper.getBeatmap(id, null)));
                } else {
                    return RouteDecision.sync(PendingMessage.ofString("用法：/m <铺面ID> [Mod]"));
                }
            }
            case "ms" -> {
                if (args.length == 1) {
                    Integer id = parsePositiveInt(args[0]);
                    if (id == null) {
                        return RouteDecision.sync(PendingMessage.ofString("用法：/ms <铺面集ID>"));
                    }
                    return queueApiRequest("ms", () -> PendingMessage.ofImageBase64(APIHelper.getBeatmapSet(id)));
                } else {
                    return RouteDecision.sync(PendingMessage.ofString("用法：/ms <铺面集ID>"));
                }
            }
            case "sms" -> {
                if (args.length == 0) {
                    return RouteDecision.sync(PendingMessage.ofString("用法：/sms <搜索关键字>"));
                }
                return queueApiRequest("sms", () -> PendingMessage.ofString(APIHelper.searchBeatmapSet(query)));
            }
            case "lb", "c" -> {
                if (args.length == 0) {
                    if (groupId != null && !groupId.isBlank()) {
                        List<Integer> groupBoundUids = UserBindingStore.findBoundUidsByGroup(platform, groupId);
                        if (groupBoundUids.isEmpty()) {
                            return RouteDecision.sync(PendingMessage.ofString("本群还没有已绑定的玩家，请先使用 /bind <玩家ID>"));
                        }
                        String[] uidArray = groupBoundUids.stream()
                                .map(String::valueOf)
                                .toArray(String[]::new);
                        return queueApiRequest("lb", () -> PendingMessage.ofImageBase64(APIHelper.getLeaderboard(uidArray)));
                    }
                    Integer uid = resolveBoundUid(platform, senderUserId);
                    if (uid == null) {
                        return RouteDecision.sync(PendingMessage.ofString("你还没有绑定玩家ID，请先使用 /bind <玩家ID>"));
                    }
                    return queueApiRequest("lb", () -> PendingMessage.ofImageBase64(APIHelper.getLeaderboard(new String[]{String.valueOf(uid)})));
                } else if (args.length == 1) {
                    Integer bm = parsePositiveInt(args[0]);
                    if (bm == null) {
                        return RouteDecision.sync(PendingMessage.ofString("用法：/lb <铺面ID> [玩家ID列表(逗号分隔)]"));
                    }
                    if (groupId != null && !groupId.isBlank()) {
                        List<Integer> groupBoundUids = UserBindingStore.findBoundUidsByGroup(platform, groupId);
                        if (groupBoundUids.isEmpty()) {
                            return RouteDecision.sync(PendingMessage.ofString("本群还没有已绑定的玩家，请先使用 /bind <玩家ID>"));
                        }
                        String[] uidArray = groupBoundUids.stream()
                                .map(String::valueOf)
                                .toArray(String[]::new);
                        return queueApiRequest("lbm", () -> PendingMessage.ofImageBase64(APIHelper.getGroupLeaderboard(bm, uidArray)));
                    }
                    Integer uid = resolveBoundUid(platform, senderUserId);
                    if (uid == null) {
                        return RouteDecision.sync(PendingMessage.ofString("你还没有绑定玩家ID，请先使用 /bind <玩家ID>"));
                    }
                    return queueApiRequest("lbm", () -> PendingMessage.ofImageBase64(APIHelper.getGroupLeaderboard(bm, new String[]{String.valueOf(uid)})));
                } else if (args.length == 2) {
                    Integer bm = parsePositiveInt(args[0]);
                    if (bm == null) {
                        return RouteDecision.sync(PendingMessage.ofString("用法：/lb <铺面ID> [玩家ID列表(逗号分隔)]"));
                    }
                    String[] uidTokens = args[1].split(",");
                    if (uidTokens.length == 0) {
                        return RouteDecision.sync(PendingMessage.ofString("玩家ID列表不能为空。用法：/lb <铺面ID> [玩家ID列表(逗号分隔)]"));
                    }
                    String[] uidArray = new String[uidTokens.length];
                    for (int i = 0; i < uidTokens.length; i++) {
                        Integer uid = parsePositiveInt(uidTokens[i].trim());
                        if (uid == null) {
                            return RouteDecision.sync(PendingMessage.ofString("玩家ID列表包含非法值。用法：/lb <铺面ID> [玩家ID列表(逗号分隔)]"));
                        }
                        uidArray[i] = String.valueOf(uid);
                    }
                    return queueApiRequest("lbm", () -> PendingMessage.ofImageBase64(APIHelper.getGroupLeaderboard(bm, uidArray)));
                } else {
                    return RouteDecision.sync(PendingMessage.ofString("用法：/lb <铺面ID> [玩家ID列表(逗号分隔)]"));
                }
            }
            case "status" -> {
                return RouteDecision.sync(PendingMessage.ofString("服务器状态：正常"));
            }
            case "help" -> {
                return RouteDecision.sync(PendingMessage.ofString("""
                        可用指令：
                        /bind <玩家ID> - 绑定你的玩家ID
                        /unbind - 解除你的玩家ID绑定
                        /bo <个数> [玩家ID] - 获取BoN图谱
                        /rs <个数> [玩家ID] - 获取最近成绩图谱
                        /m <铺面ID> - 获取铺面图谱
                        /ms <铺面集ID> - 获取铺面集图谱
                        /sms <关键字> - 搜索铺面集
                        /c <铺面ID> [玩家ID列表] - 获取指定铺面排行榜
                        /lb [铺面ID] - /c 的别名（省略参数时走绑定用户）
                        /daily - 获取每日挑战
                        /mp - 获取多人房间列表
                        /status - 获取服务器状态
                        /help - 显示此帮助信息
                        """));
            }
            default -> {
                return RouteDecision.sync(PendingMessage.ofString("未知指令。使用/help获取帮助。"));
            }
        }

    }

    private RouteDecision queueApiRequest(String requestType, ApiTaskExecutor executor) {
        long estimatedSeconds = API_REQUEST_STATS.estimateAndEnqueue(requestType);
        PendingMessage queuedNotice = PendingMessage.ofString("请求已加入队列，预计等待时间" + estimatedSeconds + "秒。");
        return RouteDecision.async(queuedNotice, new ApiTask(requestType, executor));
    }

    private void processApiTask(String targetId, String messageId, boolean groupMessage, ApiTask apiTask, AtomicInteger messageSeqCounter) {
        long startedAt = System.nanoTime();
        try {
            PendingMessage response = apiTask.executor().execute();
            if (response != null) {
                sendOutboundMessage(targetId, messageId, groupMessage, response, messageSeqCounter);
            }
        } catch (Exception e) {
            sendOutboundMessage(targetId, messageId, groupMessage, PendingMessage.ofString("请求处理失败，请稍后再试。"), messageSeqCounter);
            LOG.error("Failed to execute API task for message {}", messageId, e);
        } finally {
            long elapsedMillis = Math.max(1L, (System.nanoTime() - startedAt) / 1_000_000L);
            API_REQUEST_STATS.complete(apiTask.requestType(), elapsedMillis);
        }
    }

    private void sendOutboundMessage(String targetId, String messageId, boolean groupMessage, PendingMessage pendingMsg, AtomicInteger messageSeqCounter) {
        Message message = new Message();
        message.setContent(pendingMsg.getContent());
        message.setMsgType(pendingMsg.getMsgType());
        message.setMsgId(messageId);
        message.setMsgSeq(messageSeqCounter.getAndIncrement());

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
    }

    @FunctionalInterface
    private interface ApiTaskExecutor {
        PendingMessage execute();
    }

    private record ApiTask(String requestType, ApiTaskExecutor executor) {
    }

    protected record RouteDecision(PendingMessage initialMessage, ApiTask apiTask) {
        private static RouteDecision sync(PendingMessage message) {
            return new RouteDecision(message, null);
        }

        private static RouteDecision async(PendingMessage message, ApiTask apiTask) {
            return new RouteDecision(message, apiTask);
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

