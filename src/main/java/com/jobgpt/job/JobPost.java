package com.jobgpt.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_job_post_source_external_id", columnNames = {"source", "externalId"})
        }
)
public class JobPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30)
    private String source;

    private String externalId;

    @Column(nullable = false)
    private String title;

    private String company;

    private String location;

    private String experience;

    @Column(length = 1000)
    private String url;

    private String deadline;

    public JobPost(
            String source,
            String externalId,
            String title,
            String company,
            String location,
            String experience,
            String url,
            String deadline
    ) {
        this.source = source;
        this.externalId = externalId;
        this.title = title;
        this.company = company;
        this.location = location;
        this.experience = experience;
        this.url = url;
        this.deadline = deadline;
    }

    public void updateDetailsFrom(JobPost post) {
        this.title = prefer(post.title, this.title);
        this.company = prefer(post.company, this.company);
        this.location = prefer(post.location, this.location);
        this.experience = prefer(post.experience, this.experience);
        this.url = prefer(post.url, this.url);
        this.deadline = prefer(post.deadline, this.deadline);
    }

    private String prefer(String next, String current) {
        return next == null || next.isBlank() ? current : next;
    }
}
