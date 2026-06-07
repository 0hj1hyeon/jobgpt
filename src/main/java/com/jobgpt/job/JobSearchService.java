package com.jobgpt.job;

import com.jobgpt.crawler.JobCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSearchService {

    private static final int RESULT_LIMIT = 5;

    private final List<JobCrawler> crawlers;
    private final JobPostRepository jobPostRepository;

    @Transactional
    public List<JobPost> searchAndSave(JobSearchCondition condition) {
        Map<String, JobPost> merged = new LinkedHashMap<>();

        for (JobCrawler crawler : crawlers) {
            try {
                List<JobPost> posts = crawler.search(condition);
                for (JobPost post : posts) {
                    merged.putIfAbsent(uniqueKey(post), post);
                }
                log.info("Crawler completed. crawler={}, count={}", crawler.getClass().getSimpleName(), posts.size());
            } catch (Exception e) {
                log.warn("Crawler failed but search will continue. crawler={}", crawler.getClass().getSimpleName(), e);
            }
        }

        List<JobPost> saved = new ArrayList<>();
        for (JobPost post : merged.values()) {
            saved.add(saveIfAbsent(post));
            if (saved.size() >= RESULT_LIMIT) {
                break;
            }
        }

        return saved;
    }

    private JobPost saveIfAbsent(JobPost post) {
        if (jobPostRepository.existsBySourceAndExternalId(post.getSource(), post.getExternalId())) {
            log.debug("Job post already exists. source={}, externalId={}", post.getSource(), post.getExternalId());
            return jobPostRepository.findBySourceAndExternalId(post.getSource(), post.getExternalId())
                    .orElseThrow(() -> new IllegalStateException("Job post exists but could not be loaded."));
        }

        return saveNew(post);
    }

    private JobPost saveNew(JobPost post) {
        try {
            return jobPostRepository.save(post);
        } catch (DataIntegrityViolationException e) {
            return jobPostRepository.findBySourceAndExternalId(post.getSource(), post.getExternalId())
                    .orElseThrow(() -> e);
        }
    }

    private String uniqueKey(JobPost post) {
        return post.getSource() + ":" + post.getExternalId();
    }
}
