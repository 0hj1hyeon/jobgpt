package com.jobgpt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiConfig(
        String apiKey,
        String model
) {
}
