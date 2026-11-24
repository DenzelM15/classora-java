package com.classora.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradingTest {

    private final List<Grading.QuestionKey> keys = List.of(
            new Grading.QuestionKey(1, Set.of(10, 11), 10),
            new Grading.QuestionKey(2, Set.of(20, 21), 21),
            new Grading.QuestionKey(3, Set.of(30, 31), 30));

    @Test
    void countsOnlyCorrectAnswers() {
        Grading.Outcome outcome = Grading.grade(keys, Map.of(1, 10, 2, 20, 3, 30));
        assertEquals(2, outcome.score());
        assertEquals(3, outcome.totalQuestions());
        assertEquals("66.67", outcome.percentage().toPlainString());
        assertTrue(outcome.passed());
    }

    @Test
    void unansweredQuestionsScoreZero() {
        Grading.Outcome outcome = Grading.grade(keys, Map.of());
        assertEquals(0, outcome.score());
        assertFalse(outcome.passed());
        assertNull(outcome.answers().get(0).selectedOptionId());
    }

    @Test
    void anOptionFromAnotherQuestionIsIgnored() {
        Grading.Outcome outcome = Grading.grade(keys, Map.of(1, 21));
        assertEquals(0, outcome.score());
        assertNull(outcome.answers().get(0).selectedOptionId());
    }

    @Test
    void exactlyFiftyPercentPasses() {
        assertTrue(Grading.isPass(1, 2));
        assertTrue(Grading.isPass(50.0));
    }

    @Test
    void justUnderFiftyFails() {
        assertFalse(Grading.isPass(49, 100));
        assertFalse(Grading.isPass(1, 3));
        assertFalse(Grading.isPass(49.99));
    }

    @Test
    void emptyQuizNeverPasses() {
        assertFalse(Grading.isPass(0, 0));
        assertEquals("0.00", Grading.percentage(0, 0).toPlainString());
    }

    @Test
    void percentageRoundsHalfUpToTwoDecimals() {
        assertEquals("33.33", Grading.percentage(1, 3).toPlainString());
        assertEquals("66.67", Grading.percentage(2, 3).toPlainString());
        assertEquals("100.00", Grading.percentage(5, 5).toPlainString());
    }
}
