package xyz.zcraft.bot;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import xyz.zcraft.Config;
import xyz.zcraft.api.APIHelper;
import xyz.zcraft.data.FileInfo;
import xyz.zcraft.data.Message;
import xyz.zcraft.data.PendingMessage;
import xyz.zcraft.util.AccessToken;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class GatewayWebSocketClient extends WebSocketClient {
    private static final Logger LOG = LogManager.getLogger(GatewayWebSocketClient.class);
    private static final String prefix = "/";
    private final Gson gson = new Gson();
    private final Config config;
    private final Supplier<AccessToken> tokenSupplier;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    private final MessageSender messageSender;
    private final AtomicLong sequence = new AtomicLong(-1);
    private volatile boolean heartbeatAcked = true;

    public GatewayWebSocketClient(
            URI serverUri,
            Config config,
            Supplier<AccessToken> tokenSupplier,
            MessageSender messageSender
    ) {
        super(serverUri);
        this.config = config;
        this.tokenSupplier = tokenSupplier;
        this.messageSender = messageSender;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        LOG.info("Gateway connected");
    }

    @Override
    public void onMessage(String message) {
        JsonObject payload = gson.fromJson(message, JsonObject.class);
        updateSequence(payload.get("s"));

        int op = payload.get("op").getAsInt();
        switch (op) {
            case 10 -> onHello(payload.getAsJsonObject("d"));
            case 11 -> heartbeatAcked = true;
            case 0 -> onDispatch(payload);
            case 7, 9 -> {
                LOG.warn("Gateway requested reconnect/invalid session. closing current connection");
                close();
            }
            default -> LOG.debug("Ignored opcode {}", op);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOG.warn("Gateway closed. code={}, reason={}, remote={}", code, reason, remote);
        heartbeatExecutor.shutdownNow();
    }

    @Override
    public void onError(Exception ex) {
        LOG.error("Gateway error", ex);
    }

    private void onHello(JsonObject data) {
        int intervalMs = data.get("heartbeat_interval").getAsInt();
        sendIdentify();
        startHeartbeat(intervalMs);
    }

    private void onDispatch(JsonObject payload) {
        String eventType = payload.get("t").getAsString();

        switch (eventType) {
            case "C2C_MESSAGE_CREATE" -> onC2CMsg(payload);
        }
    }

    private void onC2CMsg(JsonObject payload) {
        final JsonObject data = payload.get("d").getAsJsonObject();
        final String content = data.get("content").getAsString();
        final String msgId = data.get("id").getAsString();
        final String openId = data.get("author").getAsJsonObject().get("user_openid").getAsString();

        final PendingMessage pendingMsg = route(content);

        if (pendingMsg == null) return;

        final Message message = new Message();

        message.setContent(pendingMsg.getContent());
        message.setMsgType(pendingMsg.getMsgType());
        message.setMsgId(msgId);

        if (pendingMsg.getFileUrl() != null) {
            FileInfo fileInfo = messageSender.uploadPrivateMedia(
                    openId,
                    pendingMsg.getFileType(),
                    pendingMsg.getFileUrl()
            );
            if (fileInfo == null) {
                LOG.error("Failed to upload media for message {}", msgId);
                return;
            }

            LOG.info("Media uploaded for message {}", msgId);

            message.setMedia(fileInfo);
        } else if (pendingMsg.getFileBase64() != null) {
            FileInfo fileInfo = messageSender.uploadPrivateMediaBase64(
                    openId,
                    pendingMsg.getFileType(),
                    pendingMsg.getFileBase64()
            );
            if (fileInfo == null) {
                LOG.error("Failed to upload base64 media for message {}", msgId);
                return;
            }

            LOG.info("Base64 media uploaded for message {}", msgId);

            message.setMedia(fileInfo);
        }

        messageSender.sendPrivateMessage(openId, message);
    }

    public PendingMessage route(String rawContent) {
        if (rawContent == null || !rawContent.startsWith(prefix)) {
            return null;
        }

        String body = rawContent.substring(prefix.length()).trim();
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
                    // TODO IMPLEMENT THIS
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
                    // TODO IMPLEMENT THIS
                } else {
                    return PendingMessage.ofString("用法：/bo <个数> [玩家ID]");
                }
            }
            case "c" -> {
                return PendingMessage.ofString("未找到已经绑定的玩家"); //TODO IMPLEMENT THIS
            }
            case "status" -> {
                return PendingMessage.ofString("服务器状态：正常");
            }
        }

        return PendingMessage.ofString("未知指令。使用/help获取帮助。");
    }

    private void sendIdentify() {
        JsonObject data = new JsonObject();
        data.addProperty("token", "QQBot " + tokenSupplier.get().token());
        data.addProperty("intents", config.intents());

        JsonObject payload = new JsonObject();
        payload.addProperty("op", 2);
        payload.add("d", data);

        send(payload.toString());
        LOG.info("Identify sent");
    }

    private void startHeartbeat(int intervalMs) {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (!heartbeatAcked) {
                LOG.warn("Last heartbeat was not acknowledged, closing websocket");
                close();
                return;
            }
            heartbeatAcked = false;
            sendHeartbeat();
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    private void sendHeartbeat() {
        JsonObject payload = new JsonObject();
        payload.addProperty("op", 1);
        JsonElement seq = sequence.get() < 0 ? JsonNull.INSTANCE : gson.toJsonTree(sequence.get());
        payload.add("d", seq);
        send(payload.toString());
    }

    private void updateSequence(JsonElement seq) {
        if (seq != null && !seq.isJsonNull()) {
            sequence.set(seq.getAsLong());
        }
    }
}

