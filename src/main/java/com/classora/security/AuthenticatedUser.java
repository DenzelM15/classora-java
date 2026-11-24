package com.classora.security;

import com.classora.model.Role;

/** Who is calling, as proven by a valid token. */
public record AuthenticatedUser(int userId, String email, Role role) {
}
