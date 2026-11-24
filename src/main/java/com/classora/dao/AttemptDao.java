package com.classora.dao;

import com.classora.model.QuizAttempt;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AttemptDao {

    /** Aggregates over a student's finished attempts. */
    public record StudentTotals(long attempts, double average, long passed, long failed) { }

    public long countCompleted(EntityManager em, int quizId, int studentId) {
        return em.createQuery(
                        "select count(a) from QuizAttempt a "
                                + "where a.quiz.id = :quizId and a.student.id = :studentId "
                                + "and a.completedAt is not null", Long.class)
                .setParameter("quizId", quizId)
                .setParameter("studentId", studentId)
                .getSingleResult();
    }

    public Optional<QuizAttempt> findLatestUnfinished(EntityManager em, int quizId, int studentId) {
        return em.createQuery(
                        "select a from QuizAttempt a "
                                + "where a.quiz.id = :quizId and a.student.id = :studentId "
                                + "and a.completedAt is null order by a.startedAt desc, a.id desc",
                        QuizAttempt.class)
                .setParameter("quizId", quizId)
                .setParameter("studentId", studentId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    /** Loads the attempt and locks its row, so a double submit cannot be graded twice. */
    public Optional<QuizAttempt> findOwnedForUpdate(EntityManager em, int attemptId, int quizId, int studentId) {
        return em.createQuery(
                        "select a from QuizAttempt a "
                                + "where a.id = :attemptId and a.quiz.id = :quizId and a.student.id = :studentId",
                        QuizAttempt.class)
                .setParameter("attemptId", attemptId)
                .setParameter("quizId", quizId)
                .setParameter("studentId", studentId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }

    public void save(EntityManager em, QuizAttempt attempt) {
        em.persist(attempt);
    }

    /** Finished attempts of one student, newest first. */
    public List<QuizAttempt> findCompletedByStudent(EntityManager em, int studentId) {
        return em.createQuery(
                        "select a from QuizAttempt a join fetch a.quiz "
                                + "where a.student.id = :studentId and a.completedAt is not null "
                                + "order by a.completedAt desc, a.id desc",
                        QuizAttempt.class)
                .setParameter("studentId", studentId)
                .getResultList();
    }

    public StudentTotals totalsForStudent(EntityManager em, int studentId) {
        Object[] row = em.createQuery(
                        "select count(a), avg(a.percentage), "
                                + "sum(case when a.passed = true then 1 else 0 end) "
                                + "from QuizAttempt a "
                                + "where a.student.id = :studentId and a.completedAt is not null",
                        Object[].class)
                .setParameter("studentId", studentId)
                .getSingleResult();

        long attempts = row[0] == null ? 0 : ((Number) row[0]).longValue();
        double average = row[1] == null ? 0 : ((Number) row[1]).doubleValue();
        long passed = row[2] == null ? 0 : ((Number) row[2]).longValue();
        return new StudentTotals(attempts, average, passed, attempts - passed);
    }

    /** quiz id -> finished attempts used by this student. */
    public Map<Integer, Long> completedCountsByQuiz(EntityManager em, int studentId) {
        return countsByQuiz(em, studentId, "is not null");
    }

    /** quiz id -> unfinished attempts of this student. */
    public Map<Integer, Long> unfinishedCountsByQuiz(EntityManager em, int studentId) {
        return countsByQuiz(em, studentId, "is null");
    }

    private Map<Integer, Long> countsByQuiz(EntityManager em, int studentId, String completedCondition) {
        List<Object[]> rows = em.createQuery(
                        "select a.quiz.id, count(a) from QuizAttempt a "
                                + "where a.student.id = :studentId and a.completedAt " + completedCondition
                                + " group by a.quiz.id",
                        Object[].class)
                .setParameter("studentId", studentId)
                .getResultList();
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }
}
