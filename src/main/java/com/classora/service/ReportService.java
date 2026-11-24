package com.classora.service;

import com.classora.config.Database;
import com.classora.dao.ReportDao;
import com.classora.dto.Dtos.AnalyticsResponse;
import com.classora.dto.Dtos.RankRow;
import com.classora.dto.Dtos.TeacherStudentRow;

import java.util.List;

/** Read-only performance views for teachers, admins and the student ranking. */
public class ReportService {

    private final Database db;
    private final ReportDao reports;

    public ReportService(Database db, ReportDao reports) {
        this.db = db;
        this.reports = reports;
    }

    public List<TeacherStudentRow> allStudents() {
        return report().allStudents();
    }

    public List<TeacherStudentRow> passedStudents() {
        return report().passedStudents();
    }

    public List<TeacherStudentRow> failedStudents() {
        return report().failedStudents();
    }

    public AnalyticsResponse analytics() {
        return report().analytics();
    }

    /** Names and pass/fail only; other students' marks are never included. */
    public List<RankRow> ranking() {
        return report().ranking();
    }

    private PerformanceReport report() {
        return new PerformanceReport(db.read(reports::studentStats));
    }
}
