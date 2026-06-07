package com.jobgpt.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobgpt.job.JobSearchCondition;

public record ChatIntent(
        IntentType intent,
        String keyword,
        String location,
        String experience
) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public JobSearchCondition toCondition() {
        return new JobSearchCondition(keyword, location, experience);
    }

    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ChatIntent.", e);
        }
    }
}
