package com.classora.web;

import com.classora.service.ReportService;

public class TeacherController {

    private final ReportService reports;

    public TeacherController(ReportService reports) {
        this.reports = reports;
    }

    public void register(Router router) {
        Access staff = Access.staff();

        router.get("/teacher/students", staff, ctx -> ApiResponse.ok(reports.allStudents()));
        router.get("/teacher/students/passed", staff, ctx -> ApiResponse.ok(reports.passedStudents()));
        router.get("/teacher/students/failed", staff, ctx -> ApiResponse.ok(reports.failedStudents()));
        router.get("/teacher/analytics", staff, ctx -> ApiResponse.ok(reports.analytics()));
    }
}
