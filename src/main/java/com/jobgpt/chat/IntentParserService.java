package com.jobgpt.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            intent, keyword, location, experience.
            intent must be one of:
            SEARCH_JOB, CREATE_SUBSCRIPTION, LIST_SUBSCRIPTION, DELETE_SUBSCRIPTION.
            Use null when a value is not present.
            Return JSON only. Do not include markdown or explanations.
            Example:
            User: 서울 신입 Java 백엔드 공고 찾아줘
            {"intent":"SEARCH_JOB","keyword":"Java 백엔드","location":"서울","experience":"신입"}
            User: 서울 신입 백엔드 공고 매일 알려줘
            {"intent":"CREATE_SUBSCRIPTION","keyword":"백엔드","location":"서울","experience":"신입"}
            User: 내 알림 목록 보여줘
            {"intent":"LIST_SUBSCRIPTION","keyword":null,"location":null,"experience":null}
            User: 알림 삭제해줘
            {"intent":"DELETE_SUBSCRIPTION","keyword":null,"location":null,"experience":null}
            """;

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    public ChatIntent parseIntent(String message) {
        log.info("Parsing job search intent with Gemini.");
        String response = generate(message);
        ChatIntent intent = toIntent(response);
        log.info("Gemini response parsed into ChatIntent: {}", intent.toJson());
        return intent;
    }

    public com.jobgpt.job.JobSearchCondition parse(String message) {
        return parseIntent(message).toCondition();
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

    private ChatIntent toIntent(String response) {
        try {
            return objectMapper.readValue(cleanJson(response), ChatIntent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse LLM response as ChatIntent: " + response, e);
        }
    }

    private String cleanJson(String response) {
        return response
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}
