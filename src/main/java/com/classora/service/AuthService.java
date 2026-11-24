package com.classora.service;

import com.classora.config.Database;
import com.classora.dao.StudentDao;
import com.classora.dao.UserDao;
import com.classora.dto.Dtos.LoginResponse;
import com.classora.dto.Dtos.LoginUser;
import com.classora.dto.Dtos.MeResponse;
import com.classora.dto.Dtos.UserResponse;
import com.classora.dto.Dtos.UserSummary;
import com.classora.model.AppUser;
import com.classora.model.Role;
import com.classora.model.Student;
import com.classora.security.AuthenticatedUser;
import com.classora.security.JwtService;
import com.classora.security.PasswordHasher;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Locale;
import java.util.Optional;

public class AuthService {

    static final int MIN_PASSWORD_LENGTH = 6;

    private final Database db;
    private final UserDao users;
    private final StudentDao students;
    private final PasswordHasher hasher;
    private final JwtService jwt;

    public AuthService(Database db, UserDao users, StudentDao students,
                       PasswordHasher hasher, JwtService jwt) {
        this.db = db;
        this.users = users;
        this.students = students;
        this.hasher = hasher;
        this.jwt = jwt;
    }

    /** Public sign-up always creates a student, plus their student profile. */
    public UserResponse register(String email, String password, String name, String surname) {
        if (isBlank(email) || isBlank(password)) {
            throw ApiException.badRequest("Email and password are required");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw ApiException.badRequest("Password must be at least 6 characters");
        }

        if (PasswordHasher.isTooLong(password)) {
            throw ApiException.badRequest("Password must be at most 72 characters");
        }

        String normalizedEmail = normalizeEmail(email);
        // Hash outside the transaction: BCrypt at cost 12 is deliberately slow.
        String passwordHash = hasher.hash(password);

        try {
            return db.inTransaction(em -> {
                if (users.findByEmail(em, normalizedEmail).isPresent()) {
                    throw ApiException.conflict("User already exists");
                }

                AppUser user = new AppUser(normalizedEmail, passwordHash, Role.STUDENT);
                users.save(em, user);

                students.save(em, new Student(user,
                        isBlank(name) ? "Student" : name.trim(),
                        isBlank(surname) ? "User" : surname.trim()));

                // Flush so a duplicate email that slipped past the check above fails here.
                em.flush();

                return new UserResponse("Student registered successfully",
                        new UserSummary(user.getId(), user.getEmail(), user.getRole().value()));
            });
        } catch (PersistenceException e) {
            // Two sign-ups with the same email at the same moment: the unique index wins.
            if (isConstraintViolation(e)) {
                throw ApiException.conflict("User already exists");
            }
            throw e;
        }
    }

    public LoginResponse login(String email, String password) {
        if (isBlank(email) || isBlank(password)) {
            throw ApiException.badRequest("Email and password are required");
        }

        String normalizedEmail = normalizeEmail(email);

        record Found(AppUser user, String name, String surname) { }

        Optional<Found> found = db.read(em -> users.findByEmail(em, normalizedEmail)
                .map(user -> {
                    Optional<Student> profile = students.findByUserId(em, user.getId());
                    return new Found(user,
                            profile.map(Student::getName).orElse(""),
                            profile.map(Student::getSurname).orElse(""));
                }));

        // Same message for "no such user" and "wrong password".
        if (found.isEmpty() || !hasher.matches(password, found.get().user().getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }

        AppUser user = found.get().user();
        String token = jwt.issue(user.getId(), user.getEmail(), user.getRole());

        return new LoginResponse("Login successful", token,
                new LoginUser(user.getId(), user.getEmail(), user.getRole().value(),
                        found.get().name(), found.get().surname()));
    }

    public MeResponse me(AuthenticatedUser caller) {
        return db.read(em -> {
            AppUser user = users.findById(em, caller.userId())
                    .orElseThrow(() -> ApiException.notFound("User not found"));
            Optional<Student> profile = students.findByUserId(em, user.getId());

            return new MeResponse(user.getId(), user.getEmail(), user.getRole().value(),
                    profile.map(Student::getId).orElse(null),
                    profile.map(Student::getName).orElse(null),
                    profile.map(Student::getSurname).orElse(null));
        });
    }

    private static boolean isConstraintViolation(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException) {
                return true;
            }
        }
        return false;
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
