package xyz.zcraft.util;

public record AccessToken(String token, long tokenGrantTime, long expiresIn) {
    public boolean isExpired() {
        return System.currentTimeMillis() - tokenGrantTime >= (expiresIn - 60) * 1000;
    }
}
