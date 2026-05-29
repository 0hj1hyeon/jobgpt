package com.jobgpt.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record JobSearchCondition(
        String keyword,
        String location,
        String experience
) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JobSearchCondition.", e);
        }
    }
}
