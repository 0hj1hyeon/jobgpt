package com.jobgpt.subscription;

import com.jobgpt.job.JobSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public Subscription create(String discordUserId, String discordChannelId, JobSearchCondition condition) {
        Subscription subscription = new Subscription(
                discordUserId,
                discordChannelId,
                condition.keyword(),
                condition.location(),
                condition.experience()
        );
        return subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<Subscription> findByDiscordUserId(String discordUserId) {
        return subscriptionRepository.findByDiscordUserId(discordUserId);
    }

    @Transactional
    public int deactivateAll(String discordUserId) {
        List<Subscription> subscriptions = subscriptionRepository.findByDiscordUserId(discordUserId);
        int count = 0;
        for (Subscription subscription : subscriptions) {
            if (subscription.isActive()) {
                subscription.deactivate();
                count++;
            }
        }
        return count;
    }
}
