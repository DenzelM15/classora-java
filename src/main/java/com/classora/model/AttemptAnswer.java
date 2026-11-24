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

/** The option a student picked for one question in one attempt (table: answers). */
@Entity
@Table(name = "answers")
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_option_id")
    private QuizOption selectedOption;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    protected AttemptAnswer() {
        // required by JPA
    }

    public AttemptAnswer(QuizAttempt attempt, Question question, QuizOption selectedOption, boolean correct) {
        this.attempt = attempt;
        this.question = question;
        this.selectedOption = selectedOption;
        this.correct = correct;
    }

    public Integer getId() { return id; }
    public boolean isCorrect() { return correct; }
}
