package com.classora.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizAvailabilityTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 10, 0);

    private static ApiException blocked(boolean published, LocalDateTime start, LocalDateTime end) {
        ApiException error = assertThrows(ApiException.class,
                () -> QuizAvailability.ensureOpen(published, start, end, NOW));
        assertEquals(403, error.status());
        return error;
    }

    @Test
    void publishedQuizWithNoWindowIsOpen() {
        QuizAvailability.ensureOpen(true, null, null, NOW);
    }

    @Test
    void draftQuizIsClosed() {
        assertEquals("This quiz is not currently available.", blocked(false, null, null).getMessage());
    }

    @Test
    void notOpenBeforeTheStartDate() {
        ApiException error = blocked(true, NOW.plusDays(1), null);
        assertTrue(error.getMessage().startsWith("This quiz becomes available on 24 Nov 2026, 12:00 SAST"));
    }

    @Test
    void closedAfterTheEndDate() {
        assertEquals("This quiz is no longer available.", blocked(true, null, NOW.minusMinutes(1)).getMessage());
    }

    @Test
    void openInsideTheWindow() {
        QuizAvailability.ensureOpen(true, NOW.minusHours(1), NOW.plusHours(1), NOW);
    }

    @Test
    void attemptLimitIsEnforced() {
        QuizAvailability.ensureAttemptsLeft(0, 1);
        QuizAvailability.ensureAttemptsLeft(2, 3);
        ApiException error = assertThrows(ApiException.class, () -> QuizAvailability.ensureAttemptsLeft(1, 1));
        assertEquals(403, error.status());
        assertEquals("You have used all attempts allowed for this quiz.", error.getMessage());
    }
}
