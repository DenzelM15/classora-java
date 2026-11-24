package com.classora.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** The student profile that belongs to a user with the student role. */
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String surname;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected Student() {
        // required by JPA
    }

    public Student(AppUser user, String name, String surname) {
        this.user = user;
        this.name = name;
        this.surname = surname;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public Integer getId() { return id; }
    public AppUser getUser() { return user; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void rename(String name, String surname) {
        this.name = name;
        this.surname = surname;
    }
}
