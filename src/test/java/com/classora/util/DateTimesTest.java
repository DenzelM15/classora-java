package com.classora.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateTimesTest {

    @Test
    void browserDatetimeLocalValueIsConvertedFromSastToUtc() {
        assertEquals(
                LocalDateTime.of(2026, 11, 22, 8, 0),
                DateTimes.parse("2026-11-22T10:00")
        );
    }

    @Test
    void valuesWithAnOffsetAreConvertedToUtc() {
        assertEquals(
                LocalDateTime.of(2026, 11, 22, 8, 0),
                DateTimes.parse("2026-11-22T10:00:00+02:00")
        );

        assertEquals(
                LocalDateTime.of(2026, 11, 22, 8, 0),
                DateTimes.parse("2026-11-22T08:00:00.000Z")
        );
    }

    @Test
    void outputIsIsoUtc() {
        assertEquals(
                "2026-11-22T08:00:00Z",
                DateTimes.toIso(LocalDateTime.of(2026, 11, 22, 8, 0))
        );

        assertEquals(
                "2026-11-22T08:00:00.123Z",
                DateTimes.toIso(
                        LocalDateTime.of(2026, 11, 22, 8, 0, 0, 123_456_789)
                )
        );
    }

    @Test
    void invalidTextIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> DateTimes.parse("tomorrow"));
        assertThrows(IllegalArgumentException.class, () -> DateTimes.parse(" "));
    }
}
