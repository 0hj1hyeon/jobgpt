package com.jobgpt.bot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discord.bot")
public record DiscordBotProperties(
        boolean enabled,
        String token
) {
}
