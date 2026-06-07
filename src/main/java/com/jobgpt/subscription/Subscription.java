package com.jobgpt.subscription;

import com.jobgpt.job.JobSearchCondition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discord_user_id", nullable = false)
    private String discordUserId;

    @Column(name = "discord_channel_id", nullable = false)
    private String discordChannelId;

    private String keyword;

    private String location;

    private String experience;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Subscription(
            String discordUserId,
            String discordChannelId,
            String keyword,
            String location,
            String experience
    ) {
        this.discordUserId = discordUserId;
        this.discordChannelId = discordChannelId;
        this.keyword = keyword;
        this.location = location;
        this.experience = experience;
        this.active = true;
    }

    public JobSearchCondition toCondition() {
        return new JobSearchCondition(keyword, location, experience);
    }

    public void deactivate() {
        this.active = false;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
