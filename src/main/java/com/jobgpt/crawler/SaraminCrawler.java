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
public class SaraminCrawler extends AbstractJobCrawler {

    private static final String SOURCE = "SARAMIN";
    private static final String BASE_URL = "https://www.saramin.co.kr";
    private static final Pattern REC_IDX_PATTERN = Pattern.compile("(?:rec_idx=|/)(\\d{5,})");

    @Override
    public List<JobPost> search(JobSearchCondition condition) {
        String url = buildUrl(condition);
        log.info("Crawling Saramin. url={}", url);

        try {
            Document document = get(url);
            Elements items = document.select("div.item_recruit, div.item_recruit div.area_job, div.list_item");
            Map<String, JobPost> posts = new LinkedHashMap<>();

            for (Element item : items) {
                Element titleLink = item.selectFirst("h2.job_tit a[href], a[href*=\"rec_idx=\"], a[href*=\"/zf_user/jobs/relay/view\"]");
                if (titleLink == null) {
                    continue;
                }

                String href = titleLink.attr("href");
                String externalId = extractExternalId(href);
                String title = text(titleLink);
                if (externalId.isBlank() || title.isBlank()) {
                    continue;
                }

                String company = text(item.selectFirst(".corp_name a, .area_corp strong a, .area_corp a"));
                String location = text(item.selectFirst(".job_condition span:nth-child(1), .job_sector + .job_condition span:nth-child(1)"));
                String experience = text(item.selectFirst(".job_condition span:nth-child(2)"));
                String deadline = text(item.selectFirst(".job_date .date, .support_detail .date, .date"));

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
            throw new IllegalStateException("Saramin crawling failed.", e);
        }
    }

    private String buildUrl(JobSearchCondition condition) {
        return BASE_URL + "/zf_user/search/recruit"
                + "?searchword=" + encode(mergedKeyword(condition))
                + "&recruitPage=1"
                + "&recruitPageCount=20";
    }

    private String extractExternalId(String href) {
        Matcher matcher = REC_IDX_PATTERN.matcher(href);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return href;
    }
}
