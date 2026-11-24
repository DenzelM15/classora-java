package com.classora.web;

import com.classora.model.Role;

import java.util.EnumSet;
import java.util.Set;

/** Who may call a route. */
public record Access(boolean loginRequired, Set<Role> roles) {

    public static Access publicAccess() {
        return new Access(false, Set.of());
    }

    /** Any logged-in user. */
    public static Access authenticated() {
        return new Access(true, Set.of());
    }

    public static Access roles(Role first, Role... others) {
        return new Access(true, EnumSet.of(first, others));
    }

    public static Access staff() {
        return roles(Role.TEACHER, Role.ADMIN);
    }

    public boolean allows(Role role) {
        return roles.isEmpty() || roles.contains(role);
    }
}
