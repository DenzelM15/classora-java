package com.classora.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One student's go at one quiz. It is created when the student starts
 * (completedAt == null) and finalised when they submit or time runs out.
 */
@Entity
@Table(name = "quiz_attempts")
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private int score;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected QuizAttempt() {
        // required by JPA
    }

    public QuizAttempt(Quiz quiz, Student student, int totalQuestions, LocalDateTime startedAt) {
        this.quiz = quiz;
        this.student = student;
        this.totalQuestions = totalQuestions;
        this.startedAt = startedAt;
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public void complete(int score, int totalQuestions, BigDecimal percentage,
                         boolean passed, LocalDateTime completedAt) {
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.percentage = percentage;
        this.passed = passed;
        this.completedAt = completedAt;
    }

    public Integer getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public Student getStudent() { return student; }
    public int getScore() { return score; }
    public int getTotalQuestions() { return totalQuestions; }
    public BigDecimal getPercentage() { return percentage; }
    public boolean isPassed() { return passed; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
