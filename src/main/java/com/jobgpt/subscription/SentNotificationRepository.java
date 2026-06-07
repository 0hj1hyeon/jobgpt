package com.jobgpt.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SentNotificationRepository extends JpaRepository<SentNotification, Long> {

    boolean existsByDiscordUserIdAndJobPostId(String discordUserId, Long jobPostId);
}
