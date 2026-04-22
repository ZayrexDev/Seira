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
    private static final int REPLAY_POLL_INTERVAL_MS = 5000;
    private static final int REPLAY_MAX_POLL_ATTEMPTS = 1000;

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
                throw parseHttpError(send.body(), send.statusCode(), "获取 BoN 失败");
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
                query = "/pk?of=" + target.macroType() + "&i=" + target.macroIndex() + "&us=" + target.boundUid() + "&u=" + uidsParam;
            } else {
                query = "/pk?m=" + target.explicitId() + "&u=" + uidsParam;
            }

            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + query))
                    .GET()
                    .build();

            final HttpResponse<byte[]> send = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() != 200) {
                throw parseHttpError(send.body(), send.statusCode(), "获取群排行失败");
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
                throw parseHttpError(send.body(), send.statusCode(), "获取排行失败");
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
                throw parseHttpError(send.body(), send.statusCode(), "获取每日挑战失败");
            }

            final Response r = GSON.fromJson(send.body(), Response.class);
            ensureApiSuccess(r, "获取每日挑战失败");
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
                throw parseHttpError(send.body(), send.statusCode(), "获取多人房间失败");
            }

            final Response r = GSON.fromJson(send.body(), Response.class);
            ensureApiSuccess(r, "获取多人房间失败");
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
                throw parseHttpError(send.body(), send.statusCode(), "获取最近成绩失败");
            }

            byte[] imageBytes = send.body();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getBeatmap(ShortcutTarget target, String mod) {
        try {
            final String query = getBeatmapQuery(target, mod);

            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + query))
                    .GET()
                    .build();

            final HttpResponse<byte[]> send = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() != 200) {
                throw parseHttpError(send.body(), send.statusCode(), "获取铺面失败");
            }

            byte[] imageBytes = send.body();

            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getBeatmapQuery(ShortcutTarget target, String mod) {
        String query = "/m?";
        if (target.isMacro()) {
            query += "&i=" + target.macroIndex();
            if (target.boundUid() != null) {
                query += "&of=" + target.macroType() + "&u=" + target.boundUid();
            } else if (target.macroType().equals("ms")) {
                query += "&ms=" + target.explicitId();
            }
        } else {
            query = "/m?m=" + target.explicitId();
        }

        if (mod != null) {
            query += "&mod=" + mod;
        }

        return query;
    }

    public static String getBeatmapSet(ShortcutTarget target) {
        try {
            String query;
            if (target.isMacro()) {
                query = "/ms?of=" + target.macroType() + "&i=" + target.macroIndex() + "&u=" + target.boundUid();
            } else {
                query = "/ms?ms=" + target.explicitId();
            }

            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + query))
                    .GET()
                    .build();

            final HttpResponse<byte[]> send = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() != 200) {
                throw parseHttpError(send.body(), send.statusCode(), "获取铺面集失败");
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
                query = "/s?of=" + target.macroType() + "&i=" + target.macroIndex() + "&u=" + target.boundUid();
            } else {
                query = "/s?s=" + target.explicitId();
            }

            HttpRequest localRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + query))
                    .GET()
                    .build();

            final HttpResponse<byte[]> send = CLIENT.send(localRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (send.statusCode() != 200) {
                throw parseHttpError(send.body(), send.statusCode(), "获取成绩失败");
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
                throw parseHttpError(send.body(), send.statusCode(), "搜索铺面集失败");
            }

            final Response response = GSON.fromJson(send.body(), Response.class);
            ensureApiSuccess(response, "搜索铺面集失败");
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

    public static ReplayRenderResult prepareReplayVideo(ShortcutTarget target) {
        ReplayTaskInfo taskInfo = createReplayTask(target);
        String taskId = taskInfo.taskId();
        try {
            waitReplayDone(taskId);
            return new ReplayRenderResult(ENDPOINT + "/replay/video/" + taskId, taskId);
        } catch (RuntimeException ex) {
            try {
                cleanupReplayVideo(taskId);
            } catch (RuntimeException ignored) {
            }
            throw ex;
        }
    }

    public static ReplayTaskInfo createReplayRenderTask(ShortcutTarget target) {
        return createReplayTask(target);
    }

    public static ReplayRenderResult waitReplayVideo(String taskId) {
        try {
            waitReplayDone(taskId);
            return new ReplayRenderResult(ENDPOINT + "/replay/video/" + taskId, taskId);
        } catch (RuntimeException ex) {
            try {
                cleanupReplayVideo(taskId);
            } catch (RuntimeException ignored) {
            }
            throw ex;
        }
    }

    public static void cleanupReplayVideo(String taskId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + "/replay/video/" + taskId))
                    .DELETE()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (!is2xx(response.statusCode())) {
                throw parseHttpError(response.body(), response.statusCode(), "清理回放视频失败");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Replay cleanup interrupted", e);
        }
    }

    private static ReplayTaskInfo createReplayTask(ShortcutTarget target) {
        try {
            String query = getReplayRenderQuery(target);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + query))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            Response payload = GSON.fromJson(response.body(), Response.class);
            if (!is2xx(response.statusCode())) {
                throw parseHttpError(response.body(), response.statusCode(), "回放渲染请求失败");
            }
            ensureApiSuccess(payload, "回放渲染请求失败");
            JsonObject data = requireDataObject(payload, "回放渲染请求缺少任务信息");
            if (!data.has("id") || data.get("id").isJsonNull()) {
                throw new RuntimeException("回放渲染请求缺少任务ID");
            }
            String taskId = data.get("id").getAsString();

            String status = data.has("status") && !data.get("status").isJsonNull()
                    ? data.get("status").getAsString()
                    : null;

            Integer position = data.has("position") && !data.get("position").isJsonNull()
                    ? data.get("position").getAsInt()
                    : null;

            final String title = getField(data, "title");
            final String artist = getField(data, "artist");
            final String version = getField(data, "version");
            final String username = getField(data, "username");
            final String rank = getField(data, "rank");
            final String accuracy = getField(data, "accuracy");
            final String star = getField(data, "star");

            return new ReplayTaskInfo(taskId, status, position, title + " - " + artist + " [" + version + " " + star + "]\n" + username + " | " + rank + " | " + accuracy);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Replay render request interrupted", e);
        }
    }

    private static String getField(JsonObject data, String title) {
        return data.get("score") != null && data.get("score").isJsonObject()
                ? data.get("score").getAsJsonObject().get(title).getAsString() : null;
    }

    private static void waitReplayDone(String taskId) {
        for (int attempt = 1; attempt <= REPLAY_MAX_POLL_ATTEMPTS; attempt++) {
            String status = getReplayStatus(taskId);
            if ("done".equalsIgnoreCase(status)) {
                return;
            }
            if ("failed".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status)) {
                throw new RuntimeException("回放渲染失败，状态：" + status);
            }
            try {
                Thread.sleep(REPLAY_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Replay status polling interrupted", e);
            }
        }

        throw new RuntimeException("回放渲染超时，请稍后重试。");
    }

    private static String getReplayStatus(String taskId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + "/replay/status/" + taskId))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            Response payload = GSON.fromJson(response.body(), Response.class);
            if (!is2xx(response.statusCode())) {
                throw parseHttpError(response.body(), response.statusCode(), "查询回放渲染状态失败");
            }
            ensureApiSuccess(payload, "查询回放渲染状态失败");
            JsonObject data = requireDataObject(payload, "回放渲染状态响应缺少data");
            if (!data.has("status") || data.get("status").isJsonNull()) {
                throw new RuntimeException("回放渲染状态响应缺少status");
            }
            return data.get("status").getAsString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Replay status request interrupted", e);
        }
    }

    private static String getReplayRenderQuery(ShortcutTarget target) {
        if (target.isMacro()) {
            if (target.boundUid() == null) {
                throw new RuntimeException("回放仅支持玩家快捷查询（如 rs1/bo1）或成绩ID。");
            }
            return "/replay/render?of=" + target.macroType() + "&i=" + target.macroIndex() + "&u=" + target.boundUid();
        }
        return "/replay/render?s=" + target.explicitId();
    }

    private static void ensureApiSuccess(Response payload, String fallbackMessage) {
        if (payload == null) {
            throw new RuntimeException(fallbackMessage);
        }
        if (!payload.isSuccess()) {
            Integer errorCode = extractErrorCode(payload);
            String message = payload.getMessage() != null ? payload.getMessage() : fallbackMessage;
            throw new ApiRequestException(errorCode, message);
        }
    }

    private static RuntimeException parseHttpError(String responseBody, int statusCode, String fallbackMessage) {
        Integer errorCode = null;
        String message = null;
        try {
            JsonObject root = GSON.fromJson(responseBody, JsonObject.class);
            if (root != null) {
                if (root.has("message") && root.get("message").isJsonPrimitive()) {
                    message = root.get("message").getAsString();
                }
                // Error code is primarily returned as Response.data.code.
                if (root.has("data") && root.get("data").isJsonObject()) {
                    JsonObject data = root.getAsJsonObject("data");
                    errorCode = readCodeFromJsonObject(data);
                }
                if (errorCode == null) {
                    errorCode = readCodeFromJsonObject(root);
                }
            }
        } catch (Exception ignored) {
        }

        String resolvedMessage = (message != null && !message.isBlank())
                ? message
                : fallbackMessage + "（HTTP " + statusCode + "）";
        return new ApiRequestException(errorCode, resolvedMessage);
    }

    private static RuntimeException parseHttpError(byte[] responseBody, int statusCode, String fallbackMessage) {
        String bodyAsText = responseBody == null ? null : new String(responseBody, StandardCharsets.UTF_8);
        return parseHttpError(bodyAsText, statusCode, fallbackMessage);
    }

    private static Integer extractErrorCode(Response payload) {
        // Error code is returned as Response.data.code.
        if (payload.getData() != null && payload.getData().isJsonObject()) {
            JsonObject data = payload.getData().getAsJsonObject();
            return readCodeFromJsonObject(data);
        }
        return null;
    }

    private static boolean is2xx(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private static Integer readCodeFromJsonObject(JsonObject object) {
        if (object == null || !object.has("code") || !object.get("code").isJsonPrimitive()) {
            return null;
        }
        try {
            return object.get("code").getAsInt();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonObject requireDataObject(Response payload, String message) {
        if (payload.getData() == null || !payload.getData().isJsonObject()) {
            throw new RuntimeException(message);
        }
        return payload.getData().getAsJsonObject();
    }

    public record ReplayRenderResult(String videoUrl, String taskId) {
    }

    public record ReplayTaskInfo(String taskId, String status, Integer position, String message) {
    }
}
