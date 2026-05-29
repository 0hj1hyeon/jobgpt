package com.jobgpt.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class DiscordBotConfig {

    @Bean
    public JDA jda(
            @Value("${discord.bot.token}") String token,
            DiscordBotListener discordBotListener
    ) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalStateException("DISCORD_BOT_TOKEN environment variable is required.");
        }

        return JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(discordBotListener)
                .build();
    }
}
