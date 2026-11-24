package com.classora.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question", nullable = false)
    private String text;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @OrderBy("id")
    private List<QuizOption> options = new ArrayList<>();

    protected Question() {
        // required by JPA
    }

    public Question(Quiz quiz, String text) {
        this.quiz = quiz;
        this.text = text;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public void addOption(QuizOption option) {
        options.add(option);
    }

    public Integer getId() { return id; }
    public Quiz getQuiz() { return quiz; }
    public String getText() { return text; }
    public List<QuizOption> getOptions() { return options; }
}
