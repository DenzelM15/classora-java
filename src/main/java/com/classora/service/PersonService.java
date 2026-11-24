package com.classora.service;

import com.classora.config.Database;
import com.classora.dao.PersonDao;
import com.classora.dto.Dtos.PersonResponse;
import com.classora.dto.Dtos.PersonRow;
import com.classora.model.Person;

import java.util.List;

/** The original CRUD demo (people table). */
public class PersonService {

    private final Database db;
    private final PersonDao people;

    public PersonService(Database db, PersonDao people) {
        this.db = db;
        this.people = people;
    }

    public PersonResponse save(String name, String surname) {
        if (AuthService.isBlank(name) || AuthService.isBlank(surname)) {
            throw ApiException.badRequest("Name and surname are required");
        }
        return db.inTransaction(em -> {
            Person person = new Person(name, surname);
            people.save(em, person);
            em.flush();
            return new PersonResponse("Person saved successfully",
                    new PersonRow(person.getId(), person.getName(), person.getSurname()));
        });
    }

    public List<PersonRow> search(String term) {
        return db.read(em -> people.search(em, term).stream()
                .map(p -> new PersonRow(p.getId(), p.getName(), p.getSurname()))
                .toList());
    }
}
