package com.classora.web;

import com.classora.model.Role;
import com.classora.service.ReportService;
import com.classora.service.StudentService;

public class StudentController {

    private final StudentService students;
    private final ReportService reports;

    public StudentController(StudentService students, ReportService reports) {
        this.students = students;
        this.reports = reports;
    }

    public void register(Router router) {
        Access student = Access.roles(Role.STUDENT);

        router.put("/student/profile", student, ctx -> ApiResponse.ok(
                students.updateProfile(ctx.caller(),
                        Json.text(ctx.body(), "name"),
                        Json.text(ctx.body(), "surname"))));

        router.get("/student/dashboard", student, ctx -> ApiResponse.ok(students.dashboard(ctx.caller())));
        router.get("/student/results", student, ctx -> ApiResponse.ok(students.results(ctx.caller())));
        router.get("/student/ranking", student, ctx -> ApiResponse.ok(reports.ranking()));
    }
}
