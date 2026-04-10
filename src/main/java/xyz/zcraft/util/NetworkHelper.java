package xyz.zcraft.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xyz.zcraft.Config;
import xyz.zcraft.data.FileInfo;
import xyz.zcraft.data.Message;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NetworkHelper {
    private static final String ENDPOINT = "https://api.sgroup.qq.com";
    private static final HttpClient CLIENT = HttpClient.newBuilder().build();
    private static final Gson gson = new Gson();

    public static String getWSSEndpoint(AccessToken accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(ENDPOINT + "/gateway"))
                    .header("Authorization", "QQBot " + accessToken.token())
                    .GET()
                    .build();

            return JsonParser.parseString(CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body())
                    .getAsJsonObject()
                    .get("url")
                    .getAsString();
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static AccessToken getAccessToken(Config config) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", config.appId());
            payload.addProperty("clientSecret", config.appSecret());

            HttpRequest request = HttpRequest.newBuilder()
                    .header("Content-Type", "application/json")
                    .uri(URI.create("https://bots.qq.com/app/getAppAccessToken"))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            final JsonElement jsonElement = JsonParser.parseString(CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body());

            return new AccessToken(
                    jsonElement.getAsJsonObject().get("access_token").getAsString(),
                    System.currentTimeMillis(),
                    jsonElement.getAsJsonObject().get("expires_in").getAsLong()
            );
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendPrivateMessage(AccessToken accessToken, String openId, Message message) {
        try {
            final var request = newRequestBuilder(accessToken)
                    .uri(URI.create(ENDPOINT + "/v2/users/" + openId + "/messages"))
                    .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(message)))
                    .build();

            if (CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() != 200) {
                throw new RuntimeException("Failed to send private message to " + openId);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static FileInfo uploadPrivateMedia(AccessToken accessToken, String openId, int fileType, String url) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("file_type", fileType);
            payload.addProperty("url", url);
            payload.addProperty("srv_send_msg", false);

            final var request = newRequestBuilder(accessToken)
                    .uri(URI.create(ENDPOINT + "/v2/users/" + openId + "/files"))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            final String body = CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
            return gson.fromJson(body, FileInfo.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public static FileInfo uploadPrivateMediaBase64(AccessToken accessToken, String openId, int fileType, String base64) {
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("file_type", fileType);
            payload.addProperty("file_data", base64);
            payload.addProperty("srv_send_msg", false);

            final var request = newRequestBuilder(accessToken)
                    .uri(URI.create(ENDPOINT + "/v2/users/" + openId + "/files"))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            final String body = CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
            return gson.fromJson(body, FileInfo.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpRequest.Builder newRequestBuilder(AccessToken accessToken) {
        return HttpRequest.newBuilder()
                .header("Authorization", "QQBot " + accessToken.token())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }
}
