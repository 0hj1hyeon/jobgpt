package com.jobgpt.crawler;

import com.jobgpt.job.JobPost;
import com.jobgpt.job.JobSearchCondition;

import java.util.List;

public interface JobCrawler {

    List<JobPost> search(JobSearchCondition condition);
}
