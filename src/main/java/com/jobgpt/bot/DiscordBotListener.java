package com.jobgpt.bot;

import com.jobgpt.chat.ChatIntent;
import com.jobgpt.chat.IntentParserService;
import com.jobgpt.chat.IntentType;
import com.jobgpt.job.JobPost;
import com.jobgpt.job.JobSearchCondition;
import com.jobgpt.job.JobSearchService;
import com.jobgpt.subscription.Subscription;
import com.jobgpt.subscription.SubscriptionService;
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
    private final SubscriptionService subscriptionService;

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
            ChatIntent intent = intentParserService.parseIntent(message);
            log.info("Parsed chat intent: {}", intent);

            switch (intent.intent()) {
                case SEARCH_JOB -> handleSearch(event, intent.toCondition());
                case CREATE_SUBSCRIPTION -> handleCreateSubscription(event, intent.toCondition());
                case LIST_SUBSCRIPTION -> handleListSubscriptions(event);
                case DELETE_SUBSCRIPTION -> handleDeleteSubscriptions(event);
            }
        } catch (Exception e) {
            log.error("Failed to handle Discord message.", e);
            event.getChannel()
                    .sendMessage("요청을 처리하는 중 오류가 발생했습니다. 서버 로그를 확인해주세요.")
                    .queue();
        }
    }

    private void handleSearch(MessageReceivedEvent event, JobSearchCondition condition) {
        event.getChannel().sendMessage("검색 조건을 분석했습니다.\n```json\n" + condition.toJson() + "\n```").queue();

        var posts = jobSearchService.searchAndSave(condition);
        event.getChannel()
                .sendMessage(formatResults(posts))
                .queue();
    }

    private void handleCreateSubscription(MessageReceivedEvent event, JobSearchCondition condition) {
        Subscription subscription = subscriptionService.create(
                event.getAuthor().getId(),
                event.getChannel().getId(),
                condition
        );

        event.getChannel()
                .sendMessage("알림을 등록했습니다.\n" + formatSubscription(subscription))
                .queue();
    }

    private void handleListSubscriptions(MessageReceivedEvent event) {
        var subscriptions = subscriptionService.findByDiscordUserId(event.getAuthor().getId());
        event.getChannel()
                .sendMessage(formatSubscriptions(subscriptions))
                .queue();
    }

    private void handleDeleteSubscriptions(MessageReceivedEvent event) {
        int deletedCount = subscriptionService.deactivateAll(event.getAuthor().getId());
        event.getChannel()
                .sendMessage("활성 알림 " + deletedCount + "개를 삭제했습니다.")
                .queue();
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

    private String formatSubscriptions(java.util.List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return "등록된 알림이 없습니다.";
        }

        StringBuilder message = new StringBuilder("등록된 알림 목록입니다.\n");
        for (Subscription subscription : subscriptions) {
            message.append(formatSubscription(subscription)).append("\n");
        }
        return message.toString();
    }

    private String formatSubscription(Subscription subscription) {
        return "#"
                + subscription.getId()
                + " "
                + (subscription.isActive() ? "활성" : "삭제됨")
                + " / "
                + nullToDash(subscription.getLocation())
                + " / "
                + nullToDash(subscription.getExperience())
                + " / "
                + nullToDash(subscription.getKeyword());
    }
}
