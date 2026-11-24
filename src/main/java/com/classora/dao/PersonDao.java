package com.classora.dao;

import com.classora.model.Person;
import jakarta.persistence.EntityManager;

import java.util.List;

public class PersonDao {

    public void save(EntityManager em, Person person) {
        em.persist(person);
    }

    public List<Person> search(EntityManager em, String search) {
        if (search == null || search.isBlank()) {
            return em.createQuery("select p from Person p order by p.id desc", Person.class)
                    .getResultList();
        }
        return em.createQuery(
                        "select p from Person p "
                                + "where lower(p.name) like lower(:term) or lower(p.surname) like lower(:term) "
                                + "order by p.id desc", Person.class)
                .setParameter("term", "%" + search + "%")
                .getResultList();
    }
}
