package com.classora.service;

import com.classora.config.Database;
import com.classora.dao.AttemptDao;
import com.classora.dao.QuizDao;
import com.classora.dao.StudentDao;
import com.classora.dto.Dtos.StartResponse;
import com.classora.dto.Dtos.SubmitResponse;
import com.classora.dto.Dtos.SubmitResult;
import com.classora.model.AttemptAnswer;
import com.classora.model.Question;
import com.classora.model.Quiz;
import com.classora.model.QuizAttempt;
import com.classora.model.QuizOption;
import com.classora.model.Student;
import com.classora.security.AuthenticatedUser;
import com.classora.util.DateTimes;
import com.classora.util.Grading;
import jakarta.persistence.LockModeType;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Starting, resuming and submitting quiz attempts. */
public class AttemptService {

    private final Database db;
    private final QuizDao quizzes;
    private final AttemptDao attempts;
    private final StudentDao students;
    private final Clock clock;

    public AttemptService(Database db, QuizDao quizzes, AttemptDao attempts, StudentDao students, Clock clock) {
        this.db = db;
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.students = students;
        this.clock = clock;
    }

    /** Starts a new attempt, or resumes the unfinished one. */
    public StartResponse start(AuthenticatedUser caller, int quizId) {
        return db.inTransaction(em -> {
            Quiz quiz = quizzes.findById(em, quizId)
                    .orElseThrow(() -> ApiException.notFound("Quiz not found"));

            LocalDateTime now = LocalDateTime.now(clock);
            QuizAvailability.ensureOpen(quiz.isPublished(), quiz.getStartAt(), quiz.getEndAt(), now);

            Student student = students.findByUserId(em, caller.userId())
                    .orElseThrow(() -> ApiException.notFound("Student profile not found"));

            // One start at a time per student, so a double click cannot create two attempts.
            em.lock(student, LockModeType.PESSIMISTIC_WRITE);

            List<Question> questions = quizzes.questionsWithOptions(em, quizId);

            Optional<QuizAttempt> unfinished = attempts.findLatestUnfinished(em, quizId, student.getId());
            QuizAttempt attempt;

            if (unfinished.isPresent()) {
                attempt = unfinished.get();
            } else {
                // Only finished attempts count against the limit.
                QuizAvailability.ensureAttemptsLeft(
                        attempts.countCompleted(em, quizId, student.getId()),
                        quiz.effectiveAttemptsAllowed());

                if (questions.isEmpty()) {
                    throw ApiException.badRequest("This quiz has no questions.");
                }

                attempt = new QuizAttempt(quiz, student, questions.size(), now);
                attempts.save(em, attempt);
                em.flush();
            }

            if (questions.isEmpty()) {
                throw ApiException.badRequest("This quiz has no questions.");
            }

            return new StartResponse(
                    attempt.getId(),
                    DateTimes.toIso(attempt.getStartedAt()),
                    DateTimes.toEpochMillis(attempt.getStartedAt()),
                    QuizMapper.info(quiz),
                    QuizMapper.questions(questions));
        });
    }

    /**
     * Grades the attempt on the server. The server's clock decides whether time ran out;
     * late submissions are still graded, with a different message.
     *
     * @param answers question id -> chosen option id
     */
    public SubmitResponse submit(AuthenticatedUser caller, int quizId, int attemptId,
                                 Map<Integer, Integer> answers) {
        return db.inTransaction(em -> {
            Student student = students.findByUserId(em, caller.userId())
                    .orElseThrow(() -> ApiException.notFound("Student profile not found"));

            QuizAttempt attempt = attempts.findOwnedForUpdate(em, attemptId, quizId, student.getId())
                    .orElseThrow(() -> ApiException.notFound("Quiz attempt not found"));

            if (attempt.isCompleted()) {
                throw ApiException.conflict("This quiz attempt has already been submitted.");
            }

            Quiz quiz = attempt.getQuiz();
            if (!quiz.isPublished()) {
                throw ApiException.forbidden("This quiz is no longer available.");
            }
            if (attempt.getStartedAt() == null) {
                throw new ApiException(500, "Unable to determine quiz start time.");
            }

            LocalDateTime now = LocalDateTime.now(clock);
            Duration elapsed = Duration.between(attempt.getStartedAt(), now);
            Duration allowed = Duration.ofMinutes(quiz.effectiveTimeLimit());
            boolean timeExpired = elapsed.compareTo(allowed) > 0;

            List<Question> questions = quizzes.questionsWithOptions(em, quizId);
            if (questions.isEmpty()) {
                throw ApiException.badRequest("This quiz has no questions");
            }

            // A late submission is accepted (and flagged); otherwise the closing date applies.
            if (!timeExpired && quiz.getEndAt() != null && now.isAfter(quiz.getEndAt())) {
                throw ApiException.forbidden("The quiz availability period has ended.");
            }

            Grading.Outcome outcome = Grading.grade(keysFor(questions), answers);

            Map<Integer, Question> questionsById = new HashMap<>();
            Map<Integer, QuizOption> optionsById = new HashMap<>();
            for (Question question : questions) {
                questionsById.put(question.getId(), question);
                for (QuizOption option : question.getOptions()) {
                    optionsById.put(option.getId(), option);
                }
            }

            for (Grading.GradedAnswer graded : outcome.answers()) {
                QuizOption selected = graded.selectedOptionId() == null
                        ? null : optionsById.get(graded.selectedOptionId());
                em.persist(new AttemptAnswer(attempt, questionsById.get(graded.questionId()),
                        selected, graded.correct()));
            }

            attempt.complete(outcome.score(), outcome.totalQuestions(), outcome.percentage(),
                    outcome.passed(), now);

            String message = timeExpired
                    ? "Time expired. Your answers were submitted automatically."
                    : "Quiz submitted successfully";

            return new SubmitResponse(message, new SubmitResult(attempt.getId(), outcome.score(),
                    outcome.totalQuestions(), outcome.percentage(), outcome.passed()));
        });
    }

    private static List<Grading.QuestionKey> keysFor(List<Question> questions) {
        return questions.stream().map(question -> {
            Integer correct = question.getOptions().stream()
                    .filter(QuizOption::isCorrect)
                    .map(QuizOption::getId)
                    .findFirst()
                    .orElse(null);
            return new Grading.QuestionKey(question.getId(),
                    new HashSet<>(question.getOptions().stream().map(QuizOption::getId).toList()),
                    correct);
        }).toList();
    }
}
