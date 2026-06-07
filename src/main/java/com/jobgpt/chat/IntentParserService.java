package com.jobgpt.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobgpt.job.JobSearchCondition;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentParserService {

    private static final String SYSTEM_PROMPT = """
            You are an intent parser for a Korean job-search Discord chatbot.
            Convert the user's message into JSON with exactly these keys:
            keyword, location, experience.
            Use null when a value is not present.
            Return JSON only. Do not include markdown or explanations.
            Example:
            User: 서울 신입 Java 백엔드 공고 찾아줘
            {"keyword":"Java 백엔드","location":"서울","experience":"신입"}
            """;

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    public JobSearchCondition parse(String message) {
        log.info("Parsing job search intent with Gemini.");
        String response = generate(message);
        JobSearchCondition condition = toCondition(response);
        log.info("Gemini response parsed into JobSearchCondition: {}", condition.toJson());
        return condition;
    }

    private String generate(String message) {
        try {
            return chatLanguageModel.generate(SYSTEM_PROMPT + "\nUser: " + message);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Gemini API call failed. Check GEMINI_API_KEY and GEMINI_MODEL environment variables.",
                    e
            );
        }
    }

    private JobSearchCondition toCondition(String response) {
        try {
            return objectMapper.readValue(cleanJson(response), JobSearchCondition.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse LLM response as JobSearchCondition: " + response, e);
        }
    }

    private String cleanJson(String response) {
        return response
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}
