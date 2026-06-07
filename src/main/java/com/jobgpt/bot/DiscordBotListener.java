package com.jobgpt.bot;

import com.jobgpt.chat.IntentParserService;
import com.jobgpt.job.JobPost;
import com.jobgpt.job.JobSearchCondition;
import com.jobgpt.job.JobSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordBotListener extends ListenerAdapter {

    private final IntentParserService intentParserService;
    private final JobSearchService jobSearchService;

    @Override
    public void onReady(ReadyEvent event) {
        log.info("Discord bot connected");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        String message = event.getMessage().getContentRaw();
        log.info("Discord message received. author={}, message={}", event.getAuthor().getName(), message);

        try {
            log.info("Calling IntentParserService for Discord message.");
            JobSearchCondition condition = intentParserService.parse(message);
            log.info("Parsed job search condition: {}", condition);

            event.getChannel().sendMessage("검색 조건을 분석했습니다.\n```json\n" + condition.toJson() + "\n```").queue();

            var posts = jobSearchService.searchAndSave(condition);
            event.getChannel()
                    .sendMessage(formatResults(posts))
                    .queue();
        } catch (Exception e) {
            log.error("Failed to handle Discord message.", e);
            event.getChannel()
                    .sendMessage("요청을 처리하는 중 오류가 발생했습니다. 서버 로그를 확인해주세요.")
                    .queue();
        }
    }

    private String formatResults(java.util.List<JobPost> posts) {
        if (posts.isEmpty()) {
            return "검색 결과가 없습니다.";
        }

        StringBuilder message = new StringBuilder("상위 채용공고 5개입니다.\n");
        for (int i = 0; i < posts.size(); i++) {
            JobPost post = posts.get(i);
            message.append(i + 1)
                    .append(". [")
                    .append(post.getSource())
                    .append("] ")
                    .append(nullToDash(post.getTitle()))
                    .append("\n")
                    .append("   회사: ")
                    .append(nullToDash(post.getCompany()))
                    .append("\n")
                    .append("   지역/경력: ")
                    .append(nullToDash(post.getLocation()))
                    .append(" / ")
                    .append(nullToDash(post.getExperience()))
                    .append("\n")
                    .append("   마감: ")
                    .append(nullToDash(post.getDeadline()))
                    .append("\n")
                    .append("   ")
                    .append(post.getUrl())
                    .append("\n");
        }
        return message.toString();
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
