package com.classora.service;

import com.classora.dao.ReportDao.StudentStats;
import com.classora.dto.Dtos.AnalyticsResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PerformanceReportTest {

    private static StudentStats stats(int id, String name, long attempts, double average, double highest) {
        return new StudentStats(id, id + 100, name, "Surname", name.toLowerCase() + "@school.test",
                attempts, average, highest);
    }

    private final PerformanceReport report = new PerformanceReport(List.of(
            stats(1, "Ann", 2, 80, 90),
            stats(2, "Ben", 1, 40, 40),
            stats(3, "Cleo", 0, 0, 0),
            stats(4, "Dan", 3, 50, 70)));

    @Test
    void allStudentsIncludesThoseWithoutAttempts() {
        assertEquals(List.of("Ann", "Dan", "Ben", "Cleo"),
                report.allStudents().stream().map(r -> r.name()).toList());
        assertEquals(Integer.valueOf(101), report.allStudents().get(0).userId());
    }

    @Test
    void passedMeansAtLeastOneAttemptAndFiftyPercentAverage() {
        assertEquals(List.of("Ann", "Dan"), report.passedStudents().stream().map(r -> r.name()).toList());
        assertNull(report.passedStudents().get(0).userId());
    }

    @Test
    void failedExcludesStudentsWithNoAttempts() {
        assertEquals(List.of("Ben"), report.failedStudents().stream().map(r -> r.name()).toList());
    }

    @Test
    void analyticsCountsAndPerformers() {
        AnalyticsResponse analytics = report.analytics();
        assertEquals(4, analytics.totalStudents());
        assertEquals(2, analytics.passedStudents());
        assertEquals(1, analytics.failedStudents());
        assertEquals("Ann", analytics.highestPerformer().name());
        assertEquals("Ben", analytics.lowestPerformer().name());
        assertEquals("80.00", analytics.highestPercentage().toPlainString());
        assertEquals("40.00", analytics.lowestPercentage().toPlainString());
    }

    @Test
    void analyticsWithNoAttemptsHasNoPerformers() {
        AnalyticsResponse analytics = new PerformanceReport(List.of(stats(1, "Ann", 0, 0, 0))).analytics();
        assertEquals(1, analytics.totalStudents());
        assertNull(analytics.highestPerformer());
        assertEquals("0", analytics.highestPercentage().toPlainString());
    }

    @Test
    void rankingGivesEveryStudentARank() {
        var ranking = report.ranking();
        assertEquals(List.of(1, 2, 3, 4), ranking.stream().map(r -> r.rank()).toList());
        assertEquals("Ann", ranking.get(0).name());
        assertEquals("Cleo", ranking.get(3).name());
    }
}
