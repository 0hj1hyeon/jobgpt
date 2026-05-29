package com.jobgpt.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(OpenAiConfig.class)
public class LangChainConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(OpenAiConfig openAiConfig) {
        if (!StringUtils.hasText(openAiConfig.apiKey())) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is required.");
        }

        return OpenAiChatModel.builder()
                .apiKey(openAiConfig.apiKey())
                .modelName(openAiConfig.model())
                .temperature(0.0)
                .build();
    }
}
