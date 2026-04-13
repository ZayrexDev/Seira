package xyz.zcraft.platform.napcat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import xyz.zcraft.data.FileInfo;
import xyz.zcraft.data.Message;
import xyz.zcraft.platform.PlatformPrivateMessageApi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NapcatPrivateMessageApi implements PlatformPrivateMessageApi {
    private static final HttpClient CLIENT = HttpClient.newBuilder().build();
    private static final Gson GSON = new Gson();

    private final String httpEndpoint;
    private final String authToken;

    public NapcatPrivateMessageApi(String httpEndpoint, String authToken) {
        this.httpEndpoint = trimEndpoint(httpEndpoint);
        this.authToken = authToken == null ? "" : authToken.trim();
    }

    @Override
    public void sendPrivateMessage(String userId, Message message) {
        JsonObject payload = new JsonObject();
        addUserId(payload, userId);
        payload.addProperty("message", buildMessageText(message));
        post("/send_private_msg", payload);
    }

    @Override
    public void sendGroupMessage(String groupId, Message message) {
        JsonObject payload = new JsonObject();
        addGroupId(payload, groupId);
        payload.addProperty("message", buildMessageText(message));
        post("/send_group_msg", payload);
    }

    @Override
    public FileInfo uploadPrivateMedia(String userId, int fileType, String url) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setString("[CQ:image,file=" + url + "]");
        return fileInfo;
    }

    @Override
    public FileInfo uploadPrivateMediaBase64(String userId, int fileType, String base64Str) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setString("[CQ:image,file=base64://" + base64Str + "]");
        return fileInfo;
    }

    @Override
    public FileInfo uploadGroupMedia(String groupId, int fileType, String url) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setString("[CQ:image,file=" + url + "]");
        return fileInfo;
    }

    @Override
    public FileInfo uploadGroupMediaBase64(String groupId, int fileType, String base64Str) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setString("[CQ:image,file=base64://" + base64Str + "]");
        return fileInfo;
    }

    private String buildMessageText(Message message) {
        StringBuilder builder = new StringBuilder();
        if (message.getContent() != null) {
            builder.append(message.getContent());
        }
        if (message.getMedia() != null && message.getMedia().getString() != null) {
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append(message.getMedia().getString());
        }
        return builder.toString();
    }

    private void addUserId(JsonObject payload, String userId) {
        try {
            payload.addProperty("user_id", Long.parseLong(userId));
        } catch (NumberFormatException e) {
            payload.addProperty("user_id", userId);
        }
    }

    private void addGroupId(JsonObject payload, String groupId) {
        try {
            payload.addProperty("group_id", Long.parseLong(groupId));
        } catch (NumberFormatException e) {
            payload.addProperty("group_id", groupId);
        }
    }

    private void post(String path, JsonObject payload) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(httpEndpoint + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)));
            if (!authToken.isEmpty()) {
                builder.header("Authorization", "Bearer " + authToken);
            }

            HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Napcat API request failed: " + response.statusCode() + " body=" + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String trimEndpoint(String endpoint) {
        if (endpoint.endsWith("/")) {
            return endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint;
    }
}

