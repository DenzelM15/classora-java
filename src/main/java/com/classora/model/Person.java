package com.classora.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Legacy entity from the original CRUD demo (table: people). */
@Entity
@Table(name = "people")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    protected Person() {
        // required by JPA
    }

    public Person(String name, String surname) {
        this.name = name;
        this.surname = surname;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
}
