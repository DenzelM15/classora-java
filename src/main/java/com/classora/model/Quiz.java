package com.classora.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {

    public static final int DEFAULT_TIME_LIMIT_MINUTES = 30;
    public static final int DEFAULT_ATTEMPTS_ALLOWED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "time_limit")
    private Integer timeLimit;

    @Column(name = "attempts_allowed")
    private Integer attemptsAllowed;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    private Boolean published = Boolean.FALSE;

    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @OrderBy("id")
    private List<Question> questions = new ArrayList<>();

    protected Quiz() {
        // required by JPA
    }

    public Quiz(String title, String description, int timeLimit, int attemptsAllowed,
                LocalDateTime startAt, LocalDateTime endAt, boolean published) {
        this.title = title;
        this.description = description;
        this.timeLimit = timeLimit;
        this.attemptsAllowed = attemptsAllowed;
        this.startAt = startAt;
        this.endAt = endAt;
        this.published = published;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public void addQuestion(Question question) {
        questions.add(question);
    }

    /** Older rows may have NULL or 0 here; fall back to the defaults. */
    public int effectiveTimeLimit() {
        return timeLimit != null && timeLimit > 0 ? timeLimit : DEFAULT_TIME_LIMIT_MINUTES;
    }

    public int effectiveAttemptsAllowed() {
        return attemptsAllowed != null && attemptsAllowed > 0 ? attemptsAllowed : DEFAULT_ATTEMPTS_ALLOWED;
    }

    public boolean isPublished() {
        return Boolean.TRUE.equals(published);
    }

    public void togglePublished() {
        published = !isPublished();
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Integer getTimeLimit() { return timeLimit; }
    public Integer getAttemptsAllowed() { return attemptsAllowed; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public List<Question> getQuestions() { return questions; }
}
