package com.jobgpt.bot;

import com.jobgpt.common.MissingEnvironmentVariableException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(DiscordBotProperties.class)
public class DiscordBotConfig {

    @Bean
    @ConditionalOnProperty(prefix = "discord.bot", name = "enabled", havingValue = "true", matchIfMissing = true)
    public JDA jda(
            DiscordBotProperties discordBotProperties,
            DiscordBotListener discordBotListener
    ) {
        String token = discordBotProperties.token();
        if (!StringUtils.hasText(token)) {
            throw new MissingEnvironmentVariableException(
                    "DISCORD_BOT_TOKEN",
                    "Set it before running the Discord bot, or run with DISCORD_BOT_ENABLED=false for parser-only testing."
            );
        }

        return JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(discordBotListener)
                .build();
    }
}
