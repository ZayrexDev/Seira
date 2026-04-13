package xyz.zcraft;

import io.github.cdimascio.dotenv.Dotenv;

public record Config(
        String platform,
        String appId,
        String appSecret,
        int intents,
        String endpoint,
        String napcatWsEndpoint,
        String napcatHttpEndpoint,
        String napcatToken
) {
	private static final int DEFAULT_INTENTS = (1 << 25);
	private static final String DEFAULT_PLATFORM = "qq";

	public static Config fromEnv(Dotenv env) {
		String platform = getOrDefault(env, "SEIRA_PLATFORM", DEFAULT_PLATFORM).toLowerCase();
		String appId = "qq".equals(platform) ? require(env, "SEIRA_APPID") : optional(env, "SEIRA_APPID");
		String appSecret = "qq".equals(platform) ? require(env, "SEIRA_APPSECRET") : optional(env, "SEIRA_APPSECRET");
		int intents = parseInt(env.get("SEIRA_INTENTS"), DEFAULT_INTENTS);
		String endpoint = require(env, "SEIRA_OSTELLA_ENDPOINT");
		String napcatWsEndpoint = "napcat".equals(platform) ? require(env, "SEIRA_NAPCAT_WS_ENDPOINT") : optional(env, "SEIRA_NAPCAT_WS_ENDPOINT");
		String napcatHttpEndpoint = "napcat".equals(platform) ? require(env, "SEIRA_NAPCAT_HTTP_ENDPOINT") : optional(env, "SEIRA_NAPCAT_HTTP_ENDPOINT");
		String napcatToken = optional(env, "SEIRA_NAPCAT_TOKEN");
		return new Config(platform, appId, appSecret, intents, endpoint, napcatWsEndpoint, napcatHttpEndpoint, napcatToken);
	}

	private static String require(Dotenv env, String key) {
		String value = env.get(key);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Missing required env: " + key);
		}
		return value;
	}

	private static int parseInt(String rawValue, int fallback) {
		if (rawValue == null || rawValue.isBlank()) {
			return fallback;
		}
		return Integer.parseInt(rawValue.trim());
	}

	private static String optional(Dotenv env, String key) {
		String value = env.get(key);
		return value == null ? "" : value.trim();
	}

	private static String getOrDefault(Dotenv env, String key, String fallback) {
		String value = env.get(key);
		if (value == null || value.isBlank()) {
			return fallback;
		}
		return value.trim();
	}
}
