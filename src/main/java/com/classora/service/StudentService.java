package com.classora.service;

import com.classora.config.Database;
import com.classora.dao.AttemptDao;
import com.classora.dao.AttemptDao.StudentTotals;
import com.classora.dao.QuizDao;
import com.classora.dao.StudentDao;
import com.classora.dto.Dtos.DashboardResponse;
import com.classora.dto.Dtos.DashboardStats;
import com.classora.dto.Dtos.ProfileResponse;
import com.classora.dto.Dtos.ResultRow;
import com.classora.dto.Dtos.StudentBrief;
import com.classora.dto.Dtos.StudentRow;
import com.classora.model.Student;
import com.classora.security.AuthenticatedUser;
import com.classora.util.DateTimes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** What a logged-in student can see and change about themselves. */
public class StudentService {

    private final Database db;
    private final StudentDao students;
    private final QuizDao quizzes;
    private final AttemptDao attempts;

    public StudentService(Database db, StudentDao students, QuizDao quizzes, AttemptDao attempts) {
        this.db = db;
        this.students = students;
        this.quizzes = quizzes;
        this.attempts = attempts;
    }

    public ProfileResponse updateProfile(AuthenticatedUser caller, String name, String surname) {
        if (AuthService.isBlank(name) || AuthService.isBlank(surname)) {
            throw ApiException.badRequest("Name and surname are required");
        }
        return db.inTransaction(em -> {
            Student student = students.findByUserId(em, caller.userId())
                    .orElseThrow(() -> ApiException.notFound("Student profile not found"));
            student.rename(name.trim(), surname.trim());

            return new ProfileResponse("Profile updated", new StudentRow(
                    student.getId(), caller.userId(), student.getName(), student.getSurname(),
                    DateTimes.toIso(student.getCreatedAt())));
        });
    }

    public DashboardResponse dashboard(AuthenticatedUser caller) {
        return db.read(em -> {
            Student student = students.findByUserId(em, caller.userId())
                    .orElseThrow(() -> ApiException.notFound("Student profile not found"));

            StudentTotals totals = attempts.totalsForStudent(em, student.getId());

            return new DashboardResponse(
                    new StudentBrief(student.getId(), student.getName(), student.getSurname()),
                    new DashboardStats(
                            (int) quizzes.countAll(em),
                            (int) totals.attempts(),
                            BigDecimal.valueOf(totals.average()).setScale(2, RoundingMode.HALF_UP),
                            (int) totals.passed(),
                            (int) totals.failed()));
        });
    }

    /** Finished attempts only, newest first. */
    public List<ResultRow> results(AuthenticatedUser caller) {
        return db.read(em -> {
            Student student = students.findByUserId(em, caller.userId())
                    .orElseThrow(() -> ApiException.notFound("Student profile not found"));

            return attempts.findCompletedByStudent(em, student.getId()).stream()
                    .map(a -> new ResultRow(a.getId(), a.getQuiz().getTitle(), a.getScore(),
                            a.getTotalQuestions(), a.getPercentage(), a.isPassed(),
                            DateTimes.toIso(a.getCompletedAt())))
                    .toList();
        });
    }
}
