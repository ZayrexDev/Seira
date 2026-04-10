package xyz.zcraft;

import io.github.cdimascio.dotenv.Dotenv;

public record Config(String appId, String appSecret, int intents, String endpoint) {
	private static final int DEFAULT_INTENTS =  (1 << 25);

	public static Config fromEnv(Dotenv env) {
		String appId = require(env, "SEIRA_APPID");
		String appSecret = require(env, "SEIRA_APPSECRET");
		int intents = parseInt(env.get("SEIRA_INTENTS"), DEFAULT_INTENTS);
		String endpoint = require(env, "SEIRA_OSTELLA_ENDPOINT");
		return new Config(appId, appSecret, intents, endpoint);
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
}
