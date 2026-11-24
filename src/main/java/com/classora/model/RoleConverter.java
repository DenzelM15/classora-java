package com.classora.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps Role <-> the lowercase text already stored in users.role. */
@Converter
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        return role == null ? null : role.value();
    }

    @Override
    public Role convertToEntityAttribute(String value) {
        return Role.fromValue(value).orElse(Role.STUDENT);
    }
}
