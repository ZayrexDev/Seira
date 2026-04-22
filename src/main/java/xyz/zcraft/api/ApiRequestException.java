package xyz.zcraft.api;

import lombok.Getter;

@Getter
public class ApiRequestException extends RuntimeException {
    private final Integer errorCode;

    public ApiRequestException(Integer errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiRequestException(Integer errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

