package com.classora.tools;

import com.classora.config.Database;
import com.classora.config.DatabaseUrl;
import com.classora.dao.StudentDao;
import com.classora.dao.UserDao;
import com.classora.model.AppUser;
import com.classora.model.Role;
import com.classora.model.Student;
import com.classora.security.PasswordHasher;

import java.util.Locale;
import java.util.Optional;

/**
 * Command-line tool to create (or update) a user, e.g. the first admin or a teacher.
 * Public sign-up only creates students. Replaces create-user.js from the Node version.
 *
 *   mvn -q compile exec:java -Dexec.args="admin@example.com MyPassword123 admin"
 */
public final class CreateUser {

    private CreateUser() {
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            usage();
            System.exit(1);
        }

        String email = args[0].trim().toLowerCase(Locale.ROOT);
        String password = args[1];
        Optional<Role> role = Role.fromValue(args[2]);

        if (role.isEmpty()) {
            System.err.println("Invalid role. Use: student, teacher or admin");
            System.exit(1);
        }
        if (password.length() < 6) {
            System.err.println("Password must be at least 6 characters.");
            System.exit(1);
        }
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            System.err.println("DATABASE_URL is not set.");
            System.exit(1);
        }

        UserDao users = new UserDao();
        StudentDao students = new StudentDao();
        String hash = new PasswordHasher().hash(password);

        try (Database db = Database.connect(DatabaseUrl.parse(databaseUrl))) {
            String outcome = db.inTransaction(em -> {
                Optional<AppUser> existing = users.findByEmail(em, email);
                AppUser user;
                String result;

                if (existing.isPresent()) {
                    user = existing.get();
                    user.setPasswordHash(hash);
                    user.setRole(role.get());
                    result = "USER UPDATED";
                } else {
                    user = new AppUser(email, hash, role.get());
                    users.save(em, user);
                    result = "USER CREATED";
                }

                if (role.get() == Role.STUDENT && students.findByUserId(em, user.getId()).isEmpty()) {
                    students.save(em, new Student(user, "Student", "User"));
                }
                em.flush();
                return result + "\n  ID:    " + user.getId() + "\n  Email: " + email
                        + "\n  Role:  " + role.get().value();
            });
            System.out.println(outcome);
        }
    }

    private static void usage() {
        System.out.println("""
                Usage:
                  mvn -q compile exec:java -Dexec.args="<email> <password> <role>"

                Examples:
                  mvn -q compile exec:java -Dexec.args="teacher@example.com MyPassword123 teacher"
                  mvn -q compile exec:java -Dexec.args="admin@example.com MyPassword123 admin"

                Roles: student, teacher, admin. Requires the DATABASE_URL environment variable.
                """);
    }
}
