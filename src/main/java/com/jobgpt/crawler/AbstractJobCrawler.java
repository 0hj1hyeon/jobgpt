package com.jobgpt.crawler;

import com.jobgpt.job.JobSearchCondition;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

abstract class AbstractJobCrawler implements JobCrawler {

    protected Document get(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; JobGPT/0.1; +https://github.com/0hj1hyeon/jobgpt)")
                .referrer("https://www.google.com")
                .timeout(10_000)
                .get();
    }

    protected String mergedKeyword(JobSearchCondition condition) {
        StringBuilder keyword = new StringBuilder(nullToBlank(condition.keyword()));
        appendIfPresent(keyword, condition.location());
        appendIfPresent(keyword, condition.experience());
        return keyword.toString().trim();
    }

    protected String absoluteUrl(String baseUrl, String href) {
        if (href == null || href.isBlank()) {
            return "";
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        return baseUrl + (href.startsWith("/") ? href : "/" + href);
    }

    protected String encode(String value) {
        return URLEncoder.encode(nullToBlank(value), StandardCharsets.UTF_8);
    }

    protected String text(Element element) {
        return element == null ? "" : normalize(element.text());
    }

    protected String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    protected String nullToBlank(String value) {
        return value == null ? "" : value.trim();
    }

    protected boolean containsIgnoreCase(String value, String token) {
        if (value == null || token == null || token.isBlank()) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }

    private void appendIfPresent(StringBuilder builder, String value) {
        if (value != null && !value.isBlank() && !containsIgnoreCase(builder.toString(), value)) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(value.trim());
        }
    }
}
