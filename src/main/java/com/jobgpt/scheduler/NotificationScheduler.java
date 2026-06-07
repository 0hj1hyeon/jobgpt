package com.jobgpt.scheduler;

import com.jobgpt.job.JobPost;
import com.jobgpt.job.JobSearchService;
import com.jobgpt.subscription.SentNotification;
import com.jobgpt.subscription.SentNotificationRepository;
import com.jobgpt.subscription.Subscription;
import com.jobgpt.subscription.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jobgpt.notification", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationScheduler {

    private static final int NOTIFICATION_LIMIT = 5;

    private final ObjectProvider<JDA> jdaProvider;
    private final SubscriptionRepository subscriptionRepository;
    private final SentNotificationRepository sentNotificationRepository;
    private final JobSearchService jobSearchService;

    @Scheduled(
            fixedDelayString = "${jobgpt.notification.fixed-delay-ms}",
            initialDelayString = "${jobgpt.notification.fixed-delay-ms}"
    )
    @Transactional
    public void sendNewJobNotifications() {
        JDA jda = jdaProvider.getIfAvailable();
        if (jda == null) {
            log.debug("Skip notification scheduler because Discord bot is disabled.");
            return;
        }

        List<Subscription> subscriptions = subscriptionRepository.findByActiveTrue();
        log.info("Checking job notifications. activeSubscriptionCount={}", subscriptions.size());

        for (Subscription subscription : subscriptions) {
            try {
                notifySubscription(jda, subscription);
            } catch (Exception e) {
                log.warn("Failed to process subscription notification. subscriptionId={}", subscription.getId(), e);
            }
        }
    }

    private void notifySubscription(JDA jda, Subscription subscription) {
        TextChannel channel = jda.getTextChannelById(subscription.getDiscordChannelId());
        if (channel == null) {
            log.warn("Discord channel not found. subscriptionId={}, channelId={}",
                    subscription.getId(),
                    subscription.getDiscordChannelId()
            );
            return;
        }

        List<JobPost> posts = jobSearchService.searchAndSave(subscription.toCondition());
        List<JobPost> unsentPosts = posts.stream()
                .filter(post -> post.getId() != null)
                .filter(post -> !sentNotificationRepository.existsByDiscordUserIdAndJobPostId(
                        subscription.getDiscordUserId(),
                        post.getId()
                ))
                .limit(NOTIFICATION_LIMIT)
                .toList();

        if (unsentPosts.isEmpty()) {
            log.debug("No new jobs for subscription. subscriptionId={}", subscription.getId());
            return;
        }

        channel.sendMessage(formatNotification(subscription, unsentPosts)).queue();
        for (JobPost post : unsentPosts) {
            sentNotificationRepository.save(new SentNotification(subscription.getDiscordUserId(), post));
        }
        log.info("Sent job notification. subscriptionId={}, count={}", subscription.getId(), unsentPosts.size());
    }

    private String formatNotification(Subscription subscription, List<JobPost> posts) {
        StringBuilder message = new StringBuilder("새 채용공고 알림입니다.\n")
                .append("조건: ")
                .append(nullToDash(subscription.getLocation()))
                .append(" / ")
                .append(nullToDash(subscription.getExperience()))
                .append(" / ")
                .append(nullToDash(subscription.getKeyword()))
                .append("\n");

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
