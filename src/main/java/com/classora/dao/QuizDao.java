package com.classora.dao;

import com.classora.model.Question;
import com.classora.model.Quiz;
import jakarta.persistence.EntityManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class QuizDao {

    public List<Quiz> findAllNewestFirst(EntityManager em) {
        return em.createQuery("select q from Quiz q order by q.id desc", Quiz.class).getResultList();
    }

    public Optional<Quiz> findById(EntityManager em, int id) {
        return Optional.ofNullable(em.find(Quiz.class, id));
    }

    public void save(EntityManager em, Quiz quiz) {
        em.persist(quiz);
    }

    public long countAll(EntityManager em) {
        return em.createQuery("select count(q) from Quiz q", Long.class).getSingleResult();
    }

    /** quiz id -> number of questions, for the quiz list. */
    public Map<Integer, Long> questionCounts(EntityManager em) {
        List<Object[]> rows = em.createQuery(
                        "select q.quiz.id, count(q) from Question q group by q.quiz.id", Object[].class)
                .getResultList();
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }

    /** Questions with their options in one query, ordered by id. */
    public List<Question> questionsWithOptions(EntityManager em, int quizId) {
        return em.createQuery(
                        "select distinct q from Question q left join fetch q.options "
                                + "where q.quiz.id = :quizId order by q.id",
                        Question.class)
                .setParameter("quizId", quizId)
                .getResultList();
    }

    /** Questions, options and attempts are removed by ON DELETE CASCADE. */
    public void deleteById(EntityManager em, int id) {
        em.createQuery("delete from Quiz q where q.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }
}
