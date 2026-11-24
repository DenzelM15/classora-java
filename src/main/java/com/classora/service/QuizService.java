package com.classora.service;

import com.classora.config.Database;
import com.classora.dao.AttemptDao;
import com.classora.dao.QuizDao;
import com.classora.dao.StudentDao;
import com.classora.dto.Dtos.CreateQuizResponse;
import com.classora.dto.Dtos.DeleteQuizResponse;
import com.classora.dto.Dtos.QuizDetailResponse;
import com.classora.dto.Dtos.QuizRef;
import com.classora.dto.Dtos.QuizStatus;
import com.classora.dto.Dtos.QuizSummary;
import com.classora.dto.Dtos.StatusResponse;
import com.classora.model.Question;
import com.classora.model.Quiz;
import com.classora.model.QuizOption;
import com.classora.model.Student;
import com.classora.security.AuthenticatedUser;
import com.classora.util.DateTimes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class QuizService {

    /** What a teacher sends to create a quiz. Parsed leniently, validated here. */
    public record CreateQuizCommand(String title, String description, Integer timeLimit,
                                    Integer attemptsAllowed, String startAt, String endAt,
                                    boolean published, List<QuestionDraft> questions) { }

    public record QuestionDraft(String question, List<OptionDraft> options) { }

    public record OptionDraft(String text, boolean correct) { }

    private final Database db;
    private final QuizDao quizzes;
    private final AttemptDao attempts;
    private final StudentDao students;
    private final Clock clock;

    public QuizService(Database db, QuizDao quizzes, AttemptDao attempts, StudentDao students, Clock clock) {
        this.db = db;
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.students = students;
        this.clock = clock;
    }

    /** Students see published quizzes with their own attempt counts; staff see everything. */
    public List<QuizSummary> list(AuthenticatedUser caller) {
        return db.read(em -> {
            List<Quiz> all = quizzes.findAllNewestFirst(em);
            Map<Integer, Long> questionCounts = quizzes.questionCounts(em);

            if (!caller.role().isStaff()) {
                Optional<Student> student = students.findByUserId(em, caller.userId());
                Map<Integer, Long> used = student
                        .map(s -> attempts.completedCountsByQuiz(em, s.getId())).orElse(Map.of());
                Map<Integer, Long> unfinished = student
                        .map(s -> attempts.unfinishedCountsByQuiz(em, s.getId())).orElse(Map.of());

                return all.stream()
                        .filter(Quiz::isPublished)
                        .map(q -> QuizMapper.summary(q,
                                questionCounts.getOrDefault(q.getId(), 0L),
                                used.getOrDefault(q.getId(), 0L).intValue(),
                                unfinished.getOrDefault(q.getId(), 0L).intValue()))
                        .toList();
            }

            return all.stream()
                    .map(q -> QuizMapper.summary(q, questionCounts.getOrDefault(q.getId(), 0L), null, null))
                    .toList();
        });
    }

    /** A quiz with its questions. Students are checked against the availability rules. */
    public QuizDetailResponse get(AuthenticatedUser caller, int quizId) {
        return db.read(em -> {
            Quiz quiz = quizzes.findById(em, quizId)
                    .orElseThrow(() -> ApiException.notFound("Quiz not found"));

            if (!caller.role().isStaff()) {
                QuizAvailability.ensureOpen(quiz.isPublished(), quiz.getStartAt(), quiz.getEndAt(),
                        LocalDateTime.now(clock));

                Student student = students.findByUserId(em, caller.userId())
                        .orElseThrow(() -> ApiException.notFound("Student profile not found"));

                QuizAvailability.ensureAttemptsLeft(
                        attempts.countCompleted(em, quizId, student.getId()),
                        quiz.effectiveAttemptsAllowed());
            }

            return new QuizDetailResponse(QuizMapper.info(quiz),
                    QuizMapper.questions(quizzes.questionsWithOptions(em, quizId)));
        });
    }

    public CreateQuizResponse create(CreateQuizCommand cmd) {
        validate(cmd);

        LocalDateTime startAt = parseOptionalDate(cmd.startAt());
        LocalDateTime endAt = parseOptionalDate(cmd.endAt());
        if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
            throw ApiException.badRequest("End date and time must be after the start date and time");
        }

        return db.inTransaction(em -> {
            Quiz quiz = new Quiz(cmd.title().trim(),
                    cmd.description() == null ? "" : cmd.description().trim(),
                    cmd.timeLimit(), cmd.attemptsAllowed(), startAt, endAt, cmd.published());

            for (QuestionDraft draft : cmd.questions()) {
                Question question = new Question(quiz, draft.question().trim());
                for (OptionDraft option : draft.options()) {
                    question.addOption(new QuizOption(question, option.text().trim(), option.correct()));
                }
                quiz.addQuestion(question);
            }

            // Questions and options are saved with the quiz (cascade).
            quizzes.save(em, quiz);
            em.flush();
            return new CreateQuizResponse("Quiz created successfully", QuizMapper.info(quiz));
        });
    }

    /** Publishes a draft or disables a published quiz. */
    public StatusResponse toggleStatus(int quizId) {
        return db.inTransaction(em -> {
            Quiz quiz = quizzes.findById(em, quizId)
                    .orElseThrow(() -> ApiException.notFound("Quiz not found"));
            quiz.togglePublished();

            String message = quiz.isPublished()
                    ? "Quiz published successfully."
                    : "Quiz disabled successfully.";
            return new StatusResponse(message,
                    new QuizStatus(quiz.getId(), quiz.getTitle(), quiz.isPublished()));
        });
    }

    public DeleteQuizResponse delete(int quizId) {
        return db.inTransaction(em -> {
            Quiz quiz = quizzes.findById(em, quizId)
                    .orElseThrow(() -> ApiException.notFound("Quiz not found"));
            QuizRef ref = new QuizRef(quiz.getId(), quiz.getTitle());
            quizzes.deleteById(em, quizId);
            return new DeleteQuizResponse("Quiz deleted successfully.", ref);
        });
    }

    static void validate(CreateQuizCommand cmd) {
        if (cmd.title() == null || cmd.title().isBlank() || cmd.questions() == null) {
            throw ApiException.badRequest("Title and questions are required");
        }
        if (cmd.questions().isEmpty()) {
            throw ApiException.badRequest("Quiz must contain at least one question");
        }
        if (cmd.timeLimit() == null || cmd.timeLimit() < 1) {
            throw ApiException.badRequest("Time limit must be at least 1 minute");
        }
        if (cmd.attemptsAllowed() == null || cmd.attemptsAllowed() < 1) {
            throw ApiException.badRequest("Attempts allowed must be at least 1");
        }

        for (QuestionDraft question : cmd.questions()) {
            if (question.question() == null || question.question().isBlank()
                    || question.options() == null || question.options().size() < 2) {
                throw ApiException.badRequest("Each question needs at least two options");
            }
            int correct = 0;
            for (OptionDraft option : question.options()) {
                if (option.text() == null || option.text().isBlank()) {
                    throw ApiException.badRequest("Every option needs text");
                }
                if (option.correct()) {
                    correct++;
                }
            }
            if (correct != 1) {
                throw ApiException.badRequest("Each question must have exactly one correct option");
            }
        }
    }

    private static LocalDateTime parseOptionalDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return DateTimes.parse(text);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
    }
}
