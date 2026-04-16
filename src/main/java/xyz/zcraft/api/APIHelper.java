package xyz.zcraft.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import xyz.zcraft.Seira;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class APIHelper {
    private static final String ENDPOINT;
    private static final HttpClient CLIENT = HttpClient.newBuilder().build();
    private static final Gson GSON = new Gson();

    static {
        ENDPOINT = Seira.getConfig().endpoint();
    }

    public static String getBoN(int n, int uid) {
        try {
            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + "/bo?" + "n=" + n + "&u=" + uid))
                    .GET()
                    .build();
            byte[] imageBytes = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray()).body();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getGroupLeaderboard(int m, String[] uids) {
        String uidsParam = String.join(",", uids);
        try {
            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + "/pk?" + "m=" + m + "&u=" + uidsParam))
                    .GET()
                    .build();
            byte[] imageBytes = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray()).body();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getLeaderboard(String[] uids) {
        String uidsParam = String.join(",", uids);
        try {
            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + "/lb?" + "u=" + uidsParam))
                    .GET()
                    .build();
            byte[] imageBytes = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray()).body();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getDaily() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + "/daily"))
                    .GET()
                    .build();

            final Response r = GSON.fromJson(CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body(), Response.class);
            final JsonObject data = r.getData().getAsJsonObject();

            return String.format(
                    """
                            ==== 今日挑战 ====
                            %s
                            曲名: %s
                            难度: %.2f* %s
                            参与人数: %d
                            模组: %s""",
                    data.get("name").getAsString(),
                    data.get("title").getAsString(),
                    data.get("difficulty_rating").getAsFloat(),
                    data.get("version").getAsString(),
                    data.get("participant_count").getAsInt(),
                    data.get("required_mods").getAsString()
            );
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getMultiplayerRooms() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + "/mp"))
                    .GET()
                    .build();

            final Response r = GSON.fromJson(CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body(), Response.class);
            final JsonArray data = r.getData().getAsJsonArray();

            final StringBuilder sb = new StringBuilder("=== 进行中的多人游戏 ===\n");
            for (JsonElement datum : data) {
                sb.append(datum.getAsString()).append("\n");
            }

            return sb.toString().trim();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getRecent(int n, int uid) {
        try {
            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + "/rs?" + "n=" + n + "&u=" + uid))
                    .GET()
                    .build();
            byte[] imageBytes = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray()).body();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getBeatmap(int m, String mod) {
        try {
            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + "/m?" + "&m=" + m + (mod == null || mod.isBlank() ? "" : "&mod=" + mod)))
                    .GET()
                    .build();
            byte[] imageBytes = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray()).body();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
