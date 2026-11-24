package com.classora.dao;

import com.classora.model.Student;
import jakarta.persistence.EntityManager;

import java.util.Optional;

public class StudentDao {

    public Optional<Student> findByUserId(EntityManager em, int userId) {
        return em.createQuery("select s from Student s where s.user.id = :userId", Student.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst();
    }

    public void save(EntityManager em, Student student) {
        em.persist(student);
    }
}
