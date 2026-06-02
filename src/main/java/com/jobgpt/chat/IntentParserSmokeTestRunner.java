package com.jobgpt.chat;

import com.jobgpt.job.JobSearchCondition;
import com.jobgpt.job.JobSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jobgpt.test-message", name = "enabled", havingValue = "true")
public class IntentParserSmokeTestRunner implements CommandLineRunner {

    private final IntentParserService intentParserService;
    private final JobSearchService jobSearchService;

    @Value("${jobgpt.test-message.input}")
    private String input;

    @Value("${jobgpt.test-message.search-enabled}")
    private boolean searchEnabled;

    @Override
    public void run(String... args) {
        JobSearchCondition condition = intentParserService.parse(input);
        log.info("Intent parser smoke test input: {}", input);
        log.info("Intent parser smoke test output: {}", condition.toJson());
        System.out.println(condition.toJson());

        if (searchEnabled) {
            var posts = jobSearchService.searchAndSave(condition);
            log.info("Job search smoke test result count: {}", posts.size());
            posts.forEach(post -> System.out.println(
                    "[" + post.getSource() + "] "
                            + post.getTitle()
                            + " / " + post.getCompany()
                            + " / " + post.getUrl()
            ));
        }
    }
}
