package com.classora.model;

import java.util.Arrays;
import java.util.Optional;

/** The three kinds of user. The database stores the lowercase value. */
public enum Role {
    STUDENT("student"),
    TEACHER("teacher"),
    ADMIN("admin");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean isStaff() {
        return this == TEACHER || this == ADMIN;
    }

    public static Optional<Role> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(r -> r.value.equals(value))
                .findFirst();
    }
}
