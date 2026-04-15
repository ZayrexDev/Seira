package xyz.zcraft.platform.napcat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.handshake.ServerHandshake;
import xyz.zcraft.platform.AbstractCommandGatewayClient;
import xyz.zcraft.platform.PlatformMessageSender;

import java.net.URI;

public class NapcatGatewayWebSocketClient extends AbstractCommandGatewayClient {
    private static final Logger LOG = LogManager.getLogger(NapcatGatewayWebSocketClient.class);
    private static final Gson GSON = new Gson();
    private final boolean wsAuthEnabled;

    public NapcatGatewayWebSocketClient(URI serverUri, PlatformMessageSender messageSender, String authToken) {
        super(serverUri, messageSender);
        String trimmedToken = authToken == null ? "" : authToken.trim();
        this.wsAuthEnabled = !trimmedToken.isEmpty();
        if (wsAuthEnabled) {
            addHeader("Authorization", "Bearer " + trimmedToken);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        LOG.info("Napcat gateway connected. endpoint={}, wsAuthEnabled={}", getURI(), wsAuthEnabled);
    }

    @Override
    public void onMessage(String rawMessage) {
        JsonObject payload = GSON.fromJson(rawMessage, JsonObject.class);
        if (payload == null || !payload.has("post_type")) {
            return;
        }

        if (!"message".equals(payload.get("post_type").getAsString())) {
            return;
        }

        if (!payload.has("message_type")) {
            return;
        }

        String messageType = payload.get("message_type").getAsString();
        String messageId = payload.has("message_id") ? payload.get("message_id").getAsString() : "";
        String content = payload.has("raw_message") ? payload.get("raw_message").getAsString() : "";

        if ("private".equals(messageType) && payload.has("user_id")) {
            String userId = payload.get("user_id").getAsString();
            LOG.info("PRIVA {} - {}", userId, content);
            onPrivateMessageReceived(userId, messageId, content);
            return;
        }

        if ("group".equals(messageType) && payload.has("group_id")) {
            String groupId = payload.get("group_id").getAsString();
            String userId = payload.has("user_id") ? payload.get("user_id").getAsString() : "";
            LOG.info("GROUP {} - {}", groupId, content);
            onGroupMessageReceived(groupId, userId, messageId, content);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        String normalizedReason = (reason == null || reason.isBlank()) ? "<empty>" : reason;
        LOG.warn(
                "Napcat gateway closed. code={}, reason={}, remote={}, endpoint={}, wsAuthEnabled={}",
                code,
                normalizedReason,
                remote,
                getURI(),
                wsAuthEnabled
        );
    }

    @Override
    public void onError(Exception ex) {
        LOG.error("Napcat gateway error", ex);
    }
}

