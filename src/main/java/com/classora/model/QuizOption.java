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

/** One answer choice of a multiple-choice question (table: options). */
@Entity
@Table(name = "options")
public class QuizOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_text", nullable = false)
    private String text;

    @Column(name = "is_correct")
    private Boolean correct = Boolean.FALSE;

    protected QuizOption() {
        // required by JPA
    }

    public QuizOption(Question question, String text, boolean correct) {
        this.question = question;
        this.text = text;
        this.correct = correct;
    }

    public Integer getId() { return id; }
    public Question getQuestion() { return question; }
    public String getText() { return text; }
    public boolean isCorrect() { return Boolean.TRUE.equals(correct); }
}
