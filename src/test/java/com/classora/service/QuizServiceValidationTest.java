package com.classora.service;

import com.classora.service.QuizService.CreateQuizCommand;
import com.classora.service.QuizService.OptionDraft;
import com.classora.service.QuizService.QuestionDraft;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuizServiceValidationTest {

    private static QuestionDraft goodQuestion() {
        return new QuestionDraft("2 + 2?", List.of(new OptionDraft("3", false), new OptionDraft("4", true)));
    }

    private static CreateQuizCommand quiz(String title, Integer time, Integer attempts, List<QuestionDraft> questions) {
        return new CreateQuizCommand(title, "", time, attempts, null, null, false, questions);
    }

    private static String errorFor(CreateQuizCommand command) {
        ApiException error = assertThrows(ApiException.class, () -> QuizService.validate(command));
        assertEquals(400, error.status());
        return error.getMessage();
    }

    @Test
    void aGoodQuizPasses() {
        QuizService.validate(quiz("Maths", 30, 1, List.of(goodQuestion())));
    }

    @Test
    void titleAndQuestionsAreRequired() {
        assertEquals("Title and questions are required", errorFor(quiz("", 30, 1, List.of(goodQuestion()))));
        assertEquals("Title and questions are required", errorFor(quiz("Maths", 30, 1, null)));
    }

    @Test
    void atLeastOneQuestion() {
        assertEquals("Quiz must contain at least one question", errorFor(quiz("Maths", 30, 1, List.of())));
    }

    @Test
    void timeLimitAndAttemptsMustBePositive() {
        assertEquals("Time limit must be at least 1 minute", errorFor(quiz("Maths", 0, 1, List.of(goodQuestion()))));
        assertEquals("Time limit must be at least 1 minute", errorFor(quiz("Maths", null, 1, List.of(goodQuestion()))));
        assertEquals("Attempts allowed must be at least 1", errorFor(quiz("Maths", 30, 0, List.of(goodQuestion()))));
    }

    @Test
    void everyQuestionNeedsTwoOptionsWithText() {
        QuestionDraft oneOption = new QuestionDraft("Q?", List.of(new OptionDraft("A", true)));
        assertEquals("Each question needs at least two options", errorFor(quiz("T", 30, 1, List.of(oneOption))));

        QuestionDraft emptyText = new QuestionDraft("Q?", List.of(new OptionDraft("A", true), new OptionDraft(" ", false)));
        assertEquals("Every option needs text", errorFor(quiz("T", 30, 1, List.of(emptyText))));
    }

    @Test
    void exactlyOneCorrectOptionPerQuestion() {
        QuestionDraft none = new QuestionDraft("Q?", List.of(new OptionDraft("A", false), new OptionDraft("B", false)));
        QuestionDraft two = new QuestionDraft("Q?", List.of(new OptionDraft("A", true), new OptionDraft("B", true)));
        assertEquals("Each question must have exactly one correct option", errorFor(quiz("T", 30, 1, List.of(none))));
        assertEquals("Each question must have exactly one correct option", errorFor(quiz("T", 30, 1, List.of(two))));
    }
}
