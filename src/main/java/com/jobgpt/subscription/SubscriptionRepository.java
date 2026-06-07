package com.jobgpt.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByActiveTrue();

    List<Subscription> findByDiscordUserId(String discordUserId);
}
