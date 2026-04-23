package xyz.zcraft.data;

import com.google.gson.JsonObject;
import lombok.Getter;

@Getter
public enum ErrorCode {
    NO_BEATMAP_FOUND(1001),
    NO_BEATMAPSET_FOUND(1002),
    NO_USER_FOUND(1003),
    NO_SCORE_FOUND(1004),
    NO_ROOM_FOUND(1005),

    ILLEGAL_ARGUMENT(2001),

    BEATMAP_FETCH_FAILED(3001),
    BEATMAPSET_FETCH_FAILED(3002),
    USER_FETCH_FAILED(3003),
    SCORE_FETCH_FAILED(3004),

    REPLAY_UNAVAILABLE(4001),

    RENDER_QUEUE_FULL(5001);

    private final int code;

    ErrorCode(int i) {
        this.code = i;
    }

    public JsonObject toJson() {
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("code", code);
        return jsonObject;
    }

    public static ErrorCode fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return null;
    }
}
