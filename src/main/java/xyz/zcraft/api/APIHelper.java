package xyz.zcraft.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import xyz.zcraft.Seira;
import xyz.zcraft.data.SearchResultItem;
import xyz.zcraft.data.ShortcutTarget;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedList;

public class APIHelper {
    private static final String ENDPOINT;
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();
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
            final HttpResponse<byte[]> send = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() != 200) {
                throw new RuntimeException("Failed to best-of-N! Status code: " + send.statusCode());
            }

            byte[] imageBytes = send.body();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getGroupLeaderboard(ShortcutTarget target, String[] uids) {
        String uidsParam = String.join(",", uids);
        try {
            String query;
            if (target.isMacro()) {
                query = "/pk?of=" + target.getOfString() + "&us=" + target.boundUid() + "&u=" + uidsParam;
            } else {
                query = "/pk?m=" + target.explicitId() + "&u=" + uidsParam;
            }

            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + query))
                    .GET()
                    .build();

            final HttpResponse<byte[]> send = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() != 200) {
                throw new RuntimeException("Failed to get group leaderboard! Status code: " + send.statusCode());
            }

            byte[] imageBytes = send.body();

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
            final HttpResponse<byte[]> send = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() != 200) {
                throw new RuntimeException("Failed to get leaderboard! Status code: " + send.statusCode());
            }

            byte[] imageBytes = send.body();

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

            final HttpResponse<String> send = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (send.statusCode() != 200) {
                throw new RuntimeException("Failed to get daily! Status code: " + send.statusCode());
            }

            final Response r = GSON.fromJson(send.body(), Response.class);
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

            final HttpResponse<String> send = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (send.statusCode() != 200) {
                throw new RuntimeException("Failed to get multiplayer rooms! Status code: " + send.statusCode());
            }

            final Response r = GSON.fromJson(send.body(), Response.class);
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

            final HttpResponse<byte[]> send = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() != 200) {
                throw new RuntimeException("Failed to get recent! Status code: " + send.statusCode());
            }

            byte[] imageBytes = send.body();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getBeatmap(ShortcutTarget target, String mod) {
        try {
            String query;
            if (target.isMacro()) {
                query = "/m?of=" + target.getOfString() + "&u=" + target.boundUid();
            } else {
                query = "/m?m=" + target.explicitId() + (mod == null || mod.isBlank() ? "" : "&mod=" + mod);
            }

            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + query))
                    .GET()
                    .build();

            final HttpResponse<byte[]> send = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() != 200) {
                throw new RuntimeException("Failed to get beatmap! Status code: " + send.statusCode());
            }

            byte[] imageBytes = send.body();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getBeatmapSet(ShortcutTarget target) {
        try {
            String query;
            if (target.isMacro()) {
                query = "/ms?of=" + target.getOfString() + "&u=" + target.boundUid();
            } else {
                query = "/ms?ms=" + target.explicitId();
            }

            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + query))
                    .GET()
                    .build();

            final HttpResponse<byte[]> send = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() != 200) {
                throw new RuntimeException("Failed to get beatmapset! Status code: " + send.statusCode());
            }

            byte[] imageBytes = send.body();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getScore(ShortcutTarget target) {
        try {
            String query;
            if (target.isMacro()) {
                query = "/s?of=" + target.getOfString() + "&u=" + target.boundUid();
            } else {
                query = "/s?s=" + target.explicitId();
            }

            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + query))
                    .GET()
                    .build();

            final HttpResponse<byte[]> send = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() != 200) {
                throw new RuntimeException("Failed to get score! Status code: " + send.statusCode());
            }

            byte[] imageBytes = send.body();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String searchBeatmapSet(String query) {
        try {
            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + "/sms?" + "q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
                    .GET()
                    .build();

            final var send = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofString());

            if (send.statusCode() != 200) {
                throw new RuntimeException("Failed to search beatmapset! Status code: " + send.statusCode());
            }

            final Response response = GSON.fromJson(send.body(), Response.class);
            final JsonArray data = response.getData().getAsJsonArray();

            final LinkedList<SearchResultItem> items = new LinkedList<>();

            data.forEach(item -> items.add(GSON.fromJson(item, SearchResultItem.class)));

            StringBuilder sb = new StringBuilder();
            sb.append("\uD83D\uDD0D").append("搜索结果\n")
                    .append("关键字：").append(query).append("\n");

            for (int i = 0; i < Math.min(items.size(), 10); i++) {
                SearchResultItem item = items.get(i);
                sb.append(i + 1).append(". ").append(item.beatmapsetId()).append(" - ").append(item.artist()).append(" - ").append(item.title())
                        .append(" [").append(item.mapperName()).append("] ")
                        .append(String.format("(%.2f* - %.2f*)", item.minStar(), item.maxStar()))
                        .append("\n");
            }

            return sb.toString().trim();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
