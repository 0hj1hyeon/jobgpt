package com.jobgpt.subscription;

import com.jobgpt.job.JobPost;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "sent_notifications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sent_notification_user_job",
                        columnNames = {"discord_user_id", "job_post_id"}
                )
        }
)
public class SentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discord_user_id", nullable = false)
    private String discordUserId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_post_id")
    private JobPost jobPost;

    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    public SentNotification(String discordUserId, JobPost jobPost) {
        this.discordUserId = discordUserId;
        this.jobPost = jobPost;
    }

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
    }
}
