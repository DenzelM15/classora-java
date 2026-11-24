package com.classora.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure scoring rules, kept free of database code so they are easy to test. */
public final class Grading {

    /** A student passes with 50% or more. */
    public static final int PASS_PERCENTAGE = 50;

    private Grading() {
    }

    /** What the server knows about one question: its options and the right one. */
    public record QuestionKey(int questionId, Set<Integer> optionIds, Integer correctOptionId) {
    }

    /** The result for one question. selectedOptionId is null when unanswered. */
    public record GradedAnswer(int questionId, Integer selectedOptionId, boolean correct) {
    }

    public record Outcome(int score, int totalQuestions, BigDecimal percentage,
                          boolean passed, List<GradedAnswer> answers) {
    }

    /**
     * @param submitted question id -> option id chosen by the student
     */
    public static Outcome grade(List<QuestionKey> keys, Map<Integer, Integer> submitted) {
        List<GradedAnswer> graded = new ArrayList<>();
        int score = 0;

        for (QuestionKey key : keys) {
            Integer chosen = submitted.get(key.questionId());

            // An option that does not belong to this question counts as unanswered.
            if (chosen != null && !key.optionIds().contains(chosen)) {
                chosen = null;
            }

            boolean correct = chosen != null && chosen.equals(key.correctOptionId());
            if (correct) {
                score++;
            }
            graded.add(new GradedAnswer(key.questionId(), chosen, correct));
        }

        int total = keys.size();
        return new Outcome(score, total, percentage(score, total), isPass(score, total), graded);
    }

    public static BigDecimal percentage(int score, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(score)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    /** Integer maths, so 1 of 3 (33.33%) and 1 of 2 (50%) are never misjudged. */
    public static boolean isPass(int score, int total) {
        return total > 0 && score * 100 >= PASS_PERCENTAGE * total;
    }

    public static boolean isPass(double averagePercentage) {
        return averagePercentage >= PASS_PERCENTAGE;
    }
}
