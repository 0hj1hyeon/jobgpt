package com.jobgpt.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobPostRepository extends JpaRepository<JobPost, Long> {

    boolean existsBySourceAndExternalId(String source, String externalId);

    Optional<JobPost> findBySourceAndExternalId(String source, String externalId);
}
