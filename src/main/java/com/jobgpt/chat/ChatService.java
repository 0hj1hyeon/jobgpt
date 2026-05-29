package com.jobgpt.chat;

import com.jobgpt.job.JobSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final IntentParserService intentParserService;

    public JobSearchCondition handle(String message) {
        JobSearchCondition condition = intentParserService.parse(message);
        log.info("Parsed job search condition: {}", condition);
        System.out.println(condition.toJson());
        return condition;
    }
}
