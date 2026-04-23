package xyz.zcraft.config;

public record AppConfig(
        SeiraConfig seira,
        OstellaConfig ostella,
        PlatformsConfig platforms
) {}

