package com.classora.service;

import com.classora.config.Database;
import com.classora.dao.StudentDao;
import com.classora.dao.UserDao;
import com.classora.dto.Dtos.AdminUserRow;
import com.classora.dto.Dtos.UserResponse;
import com.classora.dto.Dtos.UserSummary;
import com.classora.model.AppUser;
import com.classora.model.Role;
import com.classora.model.Student;
import com.classora.security.AuthenticatedUser;
import com.classora.util.DateTimes;

import java.util.List;

public class AdminService {

    private final Database db;
    private final UserDao users;
    private final StudentDao students;

    public AdminService(Database db, UserDao users, StudentDao students) {
        this.db = db;
        this.users = users;
        this.students = students;
    }

    public List<AdminUserRow> listUsers() {
        return db.read(em -> users.listWithProfiles(em).stream()
                .map(row -> {
                    AppUser user = (AppUser) row[0];
                    Student profile = (Student) row[1];
                    return new AdminUserRow(user.getId(), user.getEmail(), user.getRole().value(),
                            DateTimes.toIso(user.getCreatedAt()),
                            profile == null ? null : profile.getName(),
                            profile == null ? null : profile.getSurname());
                })
                .toList());
    }

    public UserResponse changeRole(AuthenticatedUser caller, int userId, String roleText) {
        Role role = Role.fromValue(roleText)
                .orElseThrow(() -> ApiException.badRequest("Invalid role"));
        if (userId == caller.userId()) {
            throw ApiException.badRequest("You cannot change your own role");
        }

        return db.inTransaction(em -> {
            AppUser user = users.findById(em, userId)
                    .orElseThrow(() -> ApiException.notFound("User not found"));
            user.setRole(role);

            // Someone who becomes a student needs a student profile.
            if (role == Role.STUDENT && students.findByUserId(em, userId).isEmpty()) {
                students.save(em, new Student(user, "Student", "User"));
            }

            return new UserResponse("User role updated",
                    new UserSummary(user.getId(), user.getEmail(), user.getRole().value()));
        });
    }

    public UserResponse deleteUser(AuthenticatedUser caller, int userId) {
        if (userId == caller.userId()) {
            throw ApiException.badRequest("You cannot delete your own account");
        }
        return db.inTransaction(em -> {
            AppUser user = users.findById(em, userId)
                    .orElseThrow(() -> ApiException.notFound("User not found"));
            UserSummary summary = new UserSummary(user.getId(), user.getEmail(), user.getRole().value());
            users.deleteById(em, userId);
            return new UserResponse("User deleted successfully", summary);
        });
    }
}
