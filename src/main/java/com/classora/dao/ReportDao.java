package com.classora.dao;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;

/** One query that feeds the teacher lists, analytics and the student ranking. */
public class ReportDao {

    /** A student with the totals of their finished attempts (0 when they have none). */
    public record StudentStats(int studentId, int userId, String name, String surname, String email,
                               long attempts, double average, double highest) { }

    public List<StudentStats> studentStats(EntityManager em) {
        List<Object[]> rows = em.createQuery(
                        "select s.id, u.id, s.name, s.surname, u.email, "
                                + "count(a.id), avg(a.percentage), max(a.percentage) "
                                + "from Student s join s.user u "
                                + "left join QuizAttempt a on a.student = s and a.completedAt is not null "
                                + "group by s.id, u.id, s.name, s.surname, u.email",
                        Object[].class)
                .getResultList();

        List<StudentStats> stats = new ArrayList<>();
        for (Object[] row : rows) {
            stats.add(new StudentStats(
                    ((Number) row[0]).intValue(),
                    ((Number) row[1]).intValue(),
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    ((Number) row[5]).longValue(),
                    row[6] == null ? 0 : ((Number) row[6]).doubleValue(),
                    row[7] == null ? 0 : ((Number) row[7]).doubleValue()));
        }
        return stats;
    }
}
