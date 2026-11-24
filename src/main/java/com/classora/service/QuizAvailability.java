package com.classora.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Decides whether students may open a quiz right now. */
public final class QuizAvailability {

    private static final ZoneId SAST = ZoneId.of("Africa/Johannesburg");

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'SAST'", Locale.ENGLISH);

    private QuizAvailability() {
    }

    private static String displaySast(LocalDateTime utcValue) {
        return utcValue.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(SAST)
                .format(DISPLAY);
    }

    /** @throws ApiException 403 with the reason when the quiz is not open. */
    public static void ensureOpen(boolean published, LocalDateTime startAt,
                                  LocalDateTime endAt, LocalDateTime now) {
        if (!published) {
            throw ApiException.forbidden("This quiz is not currently available.");
        }

        if (startAt != null && now.isBefore(startAt)) {
            throw ApiException.forbidden(
                    "This quiz becomes available on " + displaySast(startAt) + "."
            );
        }

        if (endAt != null && now.isAfter(endAt)) {
            throw ApiException.forbidden("This quiz is no longer available.");
        }
    }

    public static void ensureAttemptsLeft(long attemptsUsed, int attemptsAllowed) {
        if (attemptsUsed >= attemptsAllowed) {
            throw ApiException.forbidden("You have used all attempts allowed for this quiz.");
        }
    }
}
