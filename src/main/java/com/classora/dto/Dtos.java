package com.classora.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response shapes. The JSON field names are exactly the ones the existing
 * front end reads (mixed snake_case and camelCase, as in the Node API).
 */
public final class Dtos {

    private Dtos() {
    }

    // ---------- auth and profile ----------

    public record UserSummary(int id, String email, String role) { }

    public record UserResponse(String message, UserSummary user) { }

    public record LoginUser(int id, String email, String role, String name, String surname) { }

    public record LoginResponse(String message, String token, LoginUser user) { }

    public record MeResponse(int id, String email, String role,
                             @JsonProperty("student_id") Integer studentId,
                             String name, String surname) { }

    public record StudentRow(int id,
                             @JsonProperty("user_id") int userId,
                             String name, String surname,
                             @JsonProperty("created_at") String createdAt) { }

    public record ProfileResponse(String message, StudentRow student) { }

    // ---------- student dashboard, results, ranking ----------

    public record StudentBrief(int id, String name, String surname) { }

    public record DashboardStats(@JsonProperty("total_quizzes") int totalQuizzes,
                                 int attempts,
                                 @JsonProperty("average_percentage") BigDecimal averagePercentage,
                                 int passed, int failed) { }

    public record DashboardResponse(StudentBrief student, DashboardStats statistics) { }

    public record ResultRow(int id, String title, int score,
                            @JsonProperty("total_questions") int totalQuestions,
                            BigDecimal percentage, boolean passed,
                            @JsonProperty("completed_at") String completedAt) { }

    public record RankRow(int rank, int id, String name, String surname, boolean passed) { }

    // ---------- quizzes ----------

    /** attempts_used and unfinished_attempts are only sent to students. */
    public record QuizSummary(int id, String title, String description,
                              @JsonProperty("created_at") String createdAt,
                              @JsonProperty("time_limit") Integer timeLimit,
                              @JsonProperty("attempts_allowed") Integer attemptsAllowed,
                              @JsonProperty("start_at") String startAt,
                              @JsonProperty("end_at") String endAt,
                              boolean published,
                              @JsonProperty("question_count") int questionCount,
                              @JsonInclude(JsonInclude.Include.NON_NULL)
                              @JsonProperty("attempts_used") Integer attemptsUsed,
                              @JsonInclude(JsonInclude.Include.NON_NULL)
                              @JsonProperty("unfinished_attempts") Integer unfinishedAttempts) { }

    public record QuizInfo(int id, String title, String description,
                           @JsonProperty("created_at") String createdAt,
                           @JsonProperty("time_limit") Integer timeLimit,
                           @JsonProperty("attempts_allowed") Integer attemptsAllowed,
                           @JsonProperty("start_at") String startAt,
                           @JsonProperty("end_at") String endAt,
                           boolean published) { }

    /** Options never include is_correct, so answers are not leaked to the browser. */
    public record OptionView(int id, @JsonProperty("option_text") String optionText) { }

    public record QuestionView(int id, String question, List<OptionView> options) { }

    public record QuizDetailResponse(QuizInfo quiz, List<QuestionView> questions) { }

    public record StartResponse(int attemptId, String startedAt, long startedAtMs,
                                QuizInfo quiz, List<QuestionView> questions) { }

    public record SubmitResult(int attemptId, int score, int totalQuestions,
                               BigDecimal percentage, boolean passed) { }

    public record SubmitResponse(String message, SubmitResult result) { }

    public record CreateQuizResponse(String message, QuizInfo quiz) { }

    public record QuizStatus(int id, String title, boolean published) { }

    public record StatusResponse(String message, QuizStatus quiz) { }

    public record QuizRef(int id, String title) { }

    public record DeleteQuizResponse(String message, QuizRef quiz) { }

    // ---------- teacher / admin reporting ----------

    public record TeacherStudentRow(int id,
                                    @JsonInclude(JsonInclude.Include.NON_NULL)
                                    @JsonProperty("user_id") Integer userId,
                                    String name, String surname, String email, int attempts,
                                    @JsonProperty("average_percentage") BigDecimal averagePercentage,
                                    @JsonProperty("highest_percentage") BigDecimal highestPercentage) { }

    public record Performer(String name, String surname, BigDecimal percentage) { }

    public record AnalyticsResponse(int totalStudents, int passedStudents, int failedStudents,
                                    @JsonProperty("highest_percentage") BigDecimal highestPercentage,
                                    @JsonProperty("lowest_percentage") BigDecimal lowestPercentage,
                                    Performer highestPerformer, Performer lowestPerformer) { }

    public record AdminUserRow(int id, String email, String role,
                               @JsonProperty("created_at") String createdAt,
                               String name, String surname) { }

    // ---------- legacy people CRUD, health, errors ----------

    public record PersonRow(int id, String name, String surname) { }

    public record PersonResponse(String message, PersonRow person) { }

    public record Health(String status, String message) { }

    public record ErrorBody(String error) { }
}
