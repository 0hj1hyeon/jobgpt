package com.jobgpt.crawler;

import com.jobgpt.job.JobPost;
import com.jobgpt.job.JobSearchCondition;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class JobKoreaCrawler extends AbstractJobCrawler {

    private static final String SOURCE = "JOBKOREA";
    private static final String BASE_URL = "https://www.jobkorea.co.kr";
    private static final Pattern GNO_PATTERN = Pattern.compile("(?:Gno=|GI_Read/)(\\d{5,})");
    private static final Pattern DEADLINE_PATTERN = Pattern.compile("(?:~\\s*\\d{1,2}/\\d{1,2}|D-\\d+|오늘마감|\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern EMBEDDED_JOB_PATTERN = Pattern.compile("\\{\"id\":\"\\d+\".*?\"companyTypeCode\":\\d+\\}", Pattern.DOTALL);

    @Override
    public List<JobPost> search(JobSearchCondition condition) {
        String url = buildUrl(condition);
        log.info("Crawling JobKorea. url={}", url);

        try {
            Document document = get(url);
            Map<String, JobPost> embeddedPosts = parseEmbeddedJobs(document);
            if (!embeddedPosts.isEmpty()) {
                return new ArrayList<>(embeddedPosts.values());
            }

            Elements titleLinks = document.select("a[href*=\"/Recruit/GI_Read\"], a[href*=\"GI_Read?\"]");
            Map<String, JobPost> posts = new LinkedHashMap<>();

            for (Element titleLink : titleLinks) {
                String href = titleLink.attr("href");
                String externalId = extractExternalId(href);
                String title = text(titleLink);
                if (externalId.isBlank() || title.isBlank()) {
                    continue;
                }

                Element item = closestCard(titleLink);
                String cardText = text(item);
                String company = firstPresent(
                        text(item.selectFirst("a span.text-gray700, a[href*=\"/company/\"]")),
                        text(item.selectFirst(".corp a, .post-list-corp a"))
                );
                String location = firstPresent(
                        text(item.selectFirst(".loc, .option span:nth-child(1), .post-list-info .option span:nth-child(1)")),
                        extractLocation(cardText)
                );
                String experience = firstPresent(
                        text(item.selectFirst(".exp, .option span:nth-child(2), .post-list-info .option span:nth-child(2)")),
                        extractExperience(cardText)
                );
                String deadline = firstPresent(
                        text(item.selectFirst(".date, .deadline, .side .day")),
                        extractDeadline(cardText)
                );

                posts.putIfAbsent(externalId, new JobPost(
                        SOURCE,
                        externalId,
                        title,
                        company,
                        location,
                        experience,
                        absoluteUrl(BASE_URL, href),
                        deadline
                ));
            }

            return new ArrayList<>(posts.values());
        } catch (Exception e) {
            throw new IllegalStateException("JobKorea crawling failed.", e);
        }
    }

    private String buildUrl(JobSearchCondition condition) {
        return BASE_URL + "/Search/"
                + "?stext=" + encode(mergedKeyword(condition))
                + "&tabType=recruit";
    }

    private Map<String, JobPost> parseEmbeddedJobs(Document document) {
        Map<String, JobPost> posts = new LinkedHashMap<>();
        Matcher matcher = EMBEDDED_JOB_PATTERN.matcher(document.html());

        while (matcher.find()) {
            String job = matcher.group();
            String externalId = field(job, "id");
            String title = field(job, "title");
            String company = firstPresent(field(job, "postingCompanyName"), field(job, "companyName"));
            String experience = careerName(field(job, "careerType"));
            String location = field(job, "_internal_featureLocationCode");
            String deadline = deadline(field(job, "end"));

            if (externalId.isBlank() || title.isBlank()) {
                continue;
            }

            posts.putIfAbsent(externalId, new JobPost(
                    SOURCE,
                    externalId,
                    title,
                    company,
                    location,
                    experience,
                    BASE_URL + "/Recruit/GI_Read/" + externalId,
                    deadline
            ));
        }

        return posts;
    }

    private String field(String object, String fieldName) {
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(fieldName) + "\":\"(.*?)\"").matcher(object);
        if (matcher.find()) {
            return unescape(matcher.group(1));
        }
        return "";
    }

    private String firstPresent(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private Element closestCard(Element titleLink) {
        Element card = titleLink.closest("[data-sentry-component=CardJob], div.shadow-list");
        if (card != null) {
            return card;
        }
        card = titleLink.closest("li");
        if (card != null) {
            return card;
        }
        card = titleLink.closest("article");
        if (card != null) {
            return card;
        }
        card = titleLink.closest(".list-post, .recruit-info, .item, .post");
        return card == null ? titleLink.parent() : card;
    }

    private String extractExternalId(String href) {
        Matcher matcher = GNO_PATTERN.matcher(href);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return href;
    }

    private String unescape(String value) {
        return normalize(value)
                .replace("\\u0026", "&")
                .replace("\\\"", "\"")
                .replace("\\/", "/");
    }

    private String careerName(String careerType) {
        return switch (careerType) {
            case "1" -> "신입";
            case "2" -> "경력";
            case "3" -> "신입·경력";
            default -> careerType;
        };
    }

    private String deadline(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int dateEnd = value.indexOf('T');
        return dateEnd > 0 ? value.substring(0, dateEnd) : value;
    }

    private String extractLocation(String value) {
        Matcher matcher = Pattern.compile("(서울|경기|인천|부산|대구|광주|대전|울산|세종|강원|충북|충남|전북|전남|경북|경남|제주)\\s*[가-힣]*구?시?군?").matcher(value);
        return matcher.find() ? normalize(matcher.group()) : "";
    }

    private String extractExperience(String value) {
        Matcher matcher = Pattern.compile("(신입\\s*및\\s*경력|신입·경력|신입/경력|신입|경력무관|경력\\d*년?[↑이상]*|경력)").matcher(value);
        return matcher.find() ? normalize(matcher.group()) : "";
    }

    private String extractDeadline(String value) {
        Matcher matcher = DEADLINE_PATTERN.matcher(value);
        return matcher.find() ? normalize(matcher.group()) : "";
    }
}
