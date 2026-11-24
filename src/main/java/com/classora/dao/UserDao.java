package com.classora.dao;

import com.classora.model.AppUser;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class UserDao {

    public Optional<AppUser> findByEmail(EntityManager em, String email) {
        return em.createQuery("select u from AppUser u where u.email = :email", AppUser.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    public Optional<AppUser> findById(EntityManager em, int id) {
        return Optional.ofNullable(em.find(AppUser.class, id));
    }

    public void save(EntityManager em, AppUser user) {
        em.persist(user);
    }

    /** Each row is {AppUser, Student or null}. */
    public List<Object[]> listWithProfiles(EntityManager em) {
        return em.createQuery(
                        "select u, s from AppUser u left join Student s on s.user = u order by u.id",
                        Object[].class)
                .getResultList();
    }

    /** Related rows (student profile, attempts, answers) go with it through ON DELETE CASCADE. */
    public void deleteById(EntityManager em, int id) {
        em.createQuery("delete from AppUser u where u.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }
}
