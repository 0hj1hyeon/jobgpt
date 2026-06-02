package com.jobgpt.config;

import com.jobgpt.common.MissingEnvironmentVariableException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(GeminiConfig.class)
public class LangChainConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(GeminiConfig geminiConfig) {
        if (!StringUtils.hasText(geminiConfig.apiKey())) {
            throw new MissingEnvironmentVariableException(
                    "GEMINI_API_KEY",
                    "Set it before running so Gemini can parse job-search messages."
            );
        }

        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiConfig.apiKey())
                .modelName(geminiConfig.model())
                .temperature(0.0)
                .build();
    }
}
