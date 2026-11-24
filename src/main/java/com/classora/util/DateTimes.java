package com.classora.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Timestamps are stored as UTC wall-clock time in TIMESTAMP columns.
 * Browser datetime-local values are interpreted as South African time.
 */
public final class DateTimes {

    private static final ZoneId SAST = ZoneId.of("Africa/Johannesburg");

    private DateTimes() {
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * Accepts browser datetime-local values as SAST and ISO values
     * containing an offset or Z as absolute timestamps.
     */
    public static LocalDateTime parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Date is empty");
        }

        String value = text.trim();

        try {
            return OffsetDateTime.parse(value)
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // No offset: interpret the value as South African local time.
        }

        try {
            LocalDateTime local = LocalDateTime.parse(value.replace(' ', 'T'));

            return local.atZone(SAST)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date: " + text);
        }
    }

    /** ISO-8601 in UTC with millisecond precision. */
    public static String toIso(LocalDateTime value) {
        if (value == null) {
            return null;
        }

        return value.truncatedTo(ChronoUnit.MILLIS)
                .toInstant(ZoneOffset.UTC)
                .toString();
    }

    public static long toEpochMillis(LocalDateTime value) {
        return value.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
