package com.classora.service;

import com.classora.dao.ReportDao.StudentStats;
import com.classora.dto.Dtos.AnalyticsResponse;
import com.classora.dto.Dtos.Performer;
import com.classora.dto.Dtos.RankRow;
import com.classora.dto.Dtos.TeacherStudentRow;
import com.classora.util.Grading;
import com.classora.util.Ranking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Turns per-student totals into the teacher lists, analytics and ranking.
 * Pure logic: no database access, so it is easy to test.
 */
public final class PerformanceReport {

    private static final Comparator<StudentStats> BEST_FIRST = Comparator
            .comparingDouble(StudentStats::average).reversed()
            .thenComparing(StudentStats::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparingInt(StudentStats::studentId);

    private final List<StudentStats> students;

    public PerformanceReport(List<StudentStats> students) {
        this.students = List.copyOf(students);
    }

    /** Every student, including those with no attempts yet. */
    public List<TeacherStudentRow> allStudents() {
        return students.stream().sorted(BEST_FIRST)
                .map(s -> row(s, s.userId()))
                .toList();
    }

    /** Students with at least one attempt and an average of 50% or more. */
    public List<TeacherStudentRow> passedStudents() {
        return students.stream()
                .filter(s -> s.attempts() > 0 && Grading.isPass(s.average()))
                .sorted(BEST_FIRST)
                .map(s -> row(s, null))
                .toList();
    }

    /** Students with at least one attempt and an average below 50%. */
    public List<TeacherStudentRow> failedStudents() {
        return students.stream()
                .filter(s -> s.attempts() > 0 && !Grading.isPass(s.average()))
                .sorted(BEST_FIRST)
                .map(s -> row(s, null))
                .toList();
    }

    public AnalyticsResponse analytics() {
        List<StudentStats> withAttempts = students.stream().filter(s -> s.attempts() > 0).toList();

        int passed = (int) withAttempts.stream().filter(s -> Grading.isPass(s.average())).count();
        int failed = withAttempts.size() - passed;

        Optional<StudentStats> highest = withAttempts.stream().min(BEST_FIRST);
        Optional<StudentStats> lowest = withAttempts.stream().max(BEST_FIRST);

        return new AnalyticsResponse(
                students.size(),
                passed,
                failed,
                highest.map(s -> round(s.average())).orElse(BigDecimal.ZERO),
                lowest.map(s -> round(s.average())).orElse(BigDecimal.ZERO),
                highest.map(PerformanceReport::performer).orElse(null),
                lowest.map(PerformanceReport::performer).orElse(null));
    }

    /** Ranks every student by average; ties share a rank. */
    public List<RankRow> ranking() {
        List<Ranking.Input> inputs = students.stream()
                .map(s -> new Ranking.Input(s.studentId(), s.name(), s.surname(), s.average()))
                .toList();
        return Ranking.rank(inputs).stream()
                .map(r -> new RankRow(r.rank(), r.studentId(), r.name(), r.surname(), r.passed()))
                .toList();
    }

    private static TeacherStudentRow row(StudentStats s, Integer userId) {
        return new TeacherStudentRow(s.studentId(), userId, s.name(), s.surname(), s.email(),
                (int) s.attempts(), round(s.average()), round(s.highest()));
    }

    private static Performer performer(StudentStats s) {
        return new Performer(s.name(), s.surname(), round(s.average()));
    }

    static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
