package com.classora.web;

import com.classora.config.Database;
import com.classora.config.DatabaseUrl;
import com.classora.model.AppUser;
import com.classora.model.QuizOption;
import com.classora.model.Role;
import com.classora.security.JwtService;
import com.classora.security.PasswordHasher;
import com.classora.web.Application.Request;
import com.classora.web.Application.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the real API (routing, security, services, Hibernate) against an
 * in-memory H2 database in PostgreSQL mode. No servlet container needed.
 */
class ApiIntegrationTest {

    /** A clock the tests can move forward, to test quiz time limits. */
    static final class TestClock extends Clock {
        private final AtomicLong millis = new AtomicLong(Instant.parse("2026-11-23T10:00:00Z").toEpochMilli());

        void advance(Duration duration) {
            millis.addAndGet(duration.toMillis());
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
    }

    /** Skips real BCrypt (deliberately slow) so the tests stay fast. */
    static final class FastHasher extends PasswordHasher {
        @Override public String hash(String password) { return "hashed:" + password; }
        @Override public boolean matches(String password, String hash) { return ("hashed:" + password).equals(hash); }
    }

    private static final ObjectMapper JSON = new ObjectMapper();
    private static Database db;
    private static Application app;
    private static TestClock clock;
    private static int counter = 0;

    @BeforeAll
    static void start() {
        db = Database.connect(DatabaseUrl.parse(
                "jdbc:h2:mem:classora;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"));
        clock = new TestClock();
        JwtService jwt = new JwtService("integration-test-secret-1234567890", Duration.ofHours(1), clock);
        app = new Application(db, jwt, new FastHasher(), clock);
    }

    @AfterAll
    static void stop() {
        app.close();
    }

    // ---------- helpers ----------

    private static Result call(String method, String path, String token, Object body) throws Exception {
        return app.handle(new Request(method, path,
                token == null ? null : "Bearer " + token,
                Map.of(), body == null ? null : JSON.writeValueAsString(body)));
    }

    private static JsonNode json(Result result) throws Exception {
        return JSON.readTree(result.json());
    }

    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }

    private static String uniqueEmail(String prefix) {
        return prefix + (++counter) + "@school.test";
    }

    private static String registerAndLogin(String email) throws Exception {
        assertEquals(201, call("POST", "/register", null,
                map("email", email, "password", "Secret123", "name", "Test", "surname", "Student")).status());
        Result login = call("POST", "/login", null, map("email", email, "password", "Secret123"));
        assertEquals(200, login.status());
        return json(login).get("token").asText();
    }

    private static String createStaffAndLogin(Role role) throws Exception {
        String email = uniqueEmail(role.value());
        db.inTransaction(em -> {
            em.persist(new AppUser(email, "hashed:Secret123", role));
            return null;
        });
        Result login = call("POST", "/login", null, map("email", email, "password", "Secret123"));
        assertEquals(200, login.status());
        return json(login).get("token").asText();
    }

    private static Map<String, Object> question(String text, String... options) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            list.add(map("text", options[i], "isCorrect", i == 0));   // first option is correct
        }
        return map("question", text, "options", list);
    }

    private static int createQuiz(String teacherToken, boolean published, int minutes) throws Exception {
        Result created = call("POST", "/quizzes", teacherToken, map(
                "title", "Maths " + (++counter), "description", "Basics",
                "timeLimit", minutes, "attemptsAllowed", 1, "published", published,
                "questions", List.of(question("2 + 2?", "4", "5"), question("3 + 3?", "6", "7"))));
        assertEquals(201, created.status(), created.json());
        return json(created).get("quiz").get("id").asInt();
    }

    /** question id -> id of the correct option, read straight from the database. */
    private static Map<Integer, Integer> correctOptions(int quizId) {
        return db.read(em -> {
            Map<Integer, Integer> result = new HashMap<>();
            List<QuizOption> options = em.createQuery(
                    "select o from QuizOption o where o.question.quiz.id = :id and o.correct = true",
                    QuizOption.class).setParameter("id", quizId).getResultList();
            for (QuizOption option : options) {
                result.put(option.getQuestion().getId(), option.getId());
            }
            return result;
        });
    }

    private static List<Object> answers(Map<Integer, Integer> correct, int howManyRight) {
        List<Object> answers = new ArrayList<>();
        int index = 0;
        for (Map.Entry<Integer, Integer> entry : correct.entrySet()) {
            int option = index < howManyRight ? entry.getValue() : entry.getValue() + 1000000;
            answers.add(map("questionId", entry.getKey(), "selectedOptionId", option));
            index++;
        }
        return answers;
    }

    // ---------- tests ----------

    @Test
    void healthCheckIsPublic() throws Exception {
        Result result = call("GET", "/health", null, null);
        assertEquals(200, result.status());
        assertEquals("OK", json(result).get("status").asText());
    }

    @Test
    void unknownRoutesAndWrongMethods() throws Exception {
        assertEquals(404, call("GET", "/nope", null, null).status());
        assertEquals(405, call("DELETE", "/health", null, null).status());
    }

    @Test
    void registerLoginAndMe() throws Exception {
        String email = uniqueEmail("ann");
        String token = registerAndLogin(email);

        Result me = call("GET", "/me", token, null);
        assertEquals(200, me.status());
        assertEquals(email, json(me).get("email").asText());
        assertEquals("student", json(me).get("role").asText());
        assertEquals("Test", json(me).get("name").asText());
    }

    @Test
    void registrationRules() throws Exception {
        String email = uniqueEmail("dup");
        registerAndLogin(email);

        assertEquals(409, call("POST", "/register", null,
                map("email", email.toUpperCase(), "password", "Secret123")).status());
        assertEquals(400, call("POST", "/register", null,
                map("email", uniqueEmail("x"), "password", "123")).status());
        assertEquals(400, call("POST", "/register", null, map("email", uniqueEmail("x"))).status());
    }

    @Test
    void loginFailuresShareOneMessage() throws Exception {
        String email = uniqueEmail("bob");
        registerAndLogin(email);

        Result wrongPassword = call("POST", "/login", null, map("email", email, "password", "nope-nope"));
        Result unknownUser = call("POST", "/login", null, map("email", "ghost@school.test", "password", "Secret123"));
        assertEquals(401, wrongPassword.status());
        assertEquals(401, unknownUser.status());
        assertEquals(json(wrongPassword).get("error").asText(), json(unknownUser).get("error").asText());
    }

    @Test
    void tokenChecks() throws Exception {
        assertEquals(401, call("GET", "/me", null, null).status());
        assertEquals(403, call("GET", "/me", "garbage.token.value", null).status());
    }

    @Test
    void studentsCannotUseStaffRoutes() throws Exception {
        String student = registerAndLogin(uniqueEmail("stu"));
        assertEquals(403, call("GET", "/teacher/students", student, null).status());
        assertEquals(403, call("GET", "/admin/users", student, null).status());
        assertEquals(403, call("POST", "/quizzes", student, map("title", "x", "questions", List.of())).status());
    }

    @Test
    void createQuizValidationReturnsBadRequest() throws Exception {
        String teacher = createStaffAndLogin(Role.TEACHER);
        Result noQuestions = call("POST", "/quizzes", teacher,
                map("title", "Empty", "timeLimit", 10, "attemptsAllowed", 1, "questions", List.of()));
        assertEquals(400, noQuestions.status());
        assertEquals("Quiz must contain at least one question", json(noQuestions).get("error").asText());
    }

    @Test
    void fullQuizFlow() throws Exception {
        String teacher = createStaffAndLogin(Role.TEACHER);
        String student = registerAndLogin(uniqueEmail("flow"));
        int quizId = createQuiz(teacher, true, 10);
        Map<Integer, Integer> correct = correctOptions(quizId);
        assertEquals(2, correct.size());

        // The student sees it in the list, with their own attempt counts.
        JsonNode list = json(call("GET", "/quizzes", student, null));
        JsonNode listed = null;
        for (JsonNode quiz : list) {
            if (quiz.get("id").asInt() == quizId) {
                listed = quiz;
            }
        }
        assertTrue(listed != null);
        assertEquals(2, listed.get("question_count").asInt());
        assertEquals(0, listed.get("attempts_used").asInt());

        // Correct answers are never sent to the browser.
        Result detail = call("GET", "/quizzes/" + quizId, student, null);
        assertEquals(200, detail.status());
        assertFalse(detail.json().contains("is_correct"));
        assertFalse(detail.json().contains("isCorrect"));

        // Starting twice resumes the same attempt.
        Result started = call("POST", "/quizzes/" + quizId + "/start", student, null);
        assertEquals(201, started.status(), started.json());
        int attemptId = json(started).get("attemptId").asInt();
        assertEquals(attemptId, json(call("POST", "/quizzes/" + quizId + "/start", student, null)).get("attemptId").asInt());

        // One right, one wrong: 50% is a pass.
        Result submitted = call("POST", "/quizzes/" + quizId + "/submit", student,
                map("attemptId", attemptId, "answers", answers(correct, 1)));
        assertEquals(201, submitted.status(), submitted.json());
        JsonNode result = json(submitted).get("result");
        assertEquals(1, result.get("score").asInt());
        assertEquals(2, result.get("totalQuestions").asInt());
        assertEquals(50.0, result.get("percentage").asDouble());
        assertTrue(result.get("passed").asBoolean());

        // No second submission, and the single attempt is now used up.
        assertEquals(409, call("POST", "/quizzes/" + quizId + "/submit", student,
                map("attemptId", attemptId, "answers", answers(correct, 2))).status());
        assertEquals(403, call("POST", "/quizzes/" + quizId + "/start", student, null).status());
        assertEquals(403, call("GET", "/quizzes/" + quizId, student, null).status());

        // Results, dashboard and ranking reflect it.
        JsonNode results = json(call("GET", "/student/results", student, null));
        assertEquals(1, results.size());
        assertEquals(50.0, results.get(0).get("percentage").asDouble());

        JsonNode dashboard = json(call("GET", "/student/dashboard", student, null));
        assertEquals(1, dashboard.get("statistics").get("attempts").asInt());
        assertEquals(1, dashboard.get("statistics").get("passed").asInt());
        assertEquals(50.0, dashboard.get("statistics").get("average_percentage").asDouble());

        JsonNode ranking = json(call("GET", "/student/ranking", student, null));
        assertTrue(ranking.size() >= 1);
        assertFalse(ranking.toString().contains("percentage"));

        // Teachers see the student in their lists.
        assertEquals(200, call("GET", "/teacher/students", teacher, null).status());
        assertEquals(200, call("GET", "/teacher/students/passed", teacher, null).status());
        assertEquals(200, call("GET", "/teacher/students/failed", teacher, null).status());
        JsonNode analytics = json(call("GET", "/teacher/analytics", teacher, null));
        assertTrue(analytics.get("totalStudents").asInt() >= 1);
    }

    @Test
    void lateSubmissionIsStillGradedButFlagged() throws Exception {
        String teacher = createStaffAndLogin(Role.TEACHER);
        String student = registerAndLogin(uniqueEmail("late"));
        int quizId = createQuiz(teacher, true, 5);
        Map<Integer, Integer> correct = correctOptions(quizId);

        int attemptId = json(call("POST", "/quizzes/" + quizId + "/start", student, null)).get("attemptId").asInt();
        clock.advance(Duration.ofMinutes(6));

        Result submitted = call("POST", "/quizzes/" + quizId + "/submit", student,
                map("attemptId", attemptId, "answers", answers(correct, 2)));
        assertEquals(201, submitted.status(), submitted.json());
        assertEquals("Time expired. Your answers were submitted automatically.",
                json(submitted).get("message").asText());
        assertEquals(2, json(submitted).get("result").get("score").asInt());
    }

    @Test
    void draftQuizzesAreHiddenFromStudentsUntilPublished() throws Exception {
        String teacher = createStaffAndLogin(Role.TEACHER);
        String student = registerAndLogin(uniqueEmail("draft"));
        int quizId = createQuiz(teacher, false, 10);

        assertEquals(403, call("POST", "/quizzes/" + quizId + "/start", student, null).status());
        for (JsonNode quiz : json(call("GET", "/quizzes", student, null))) {
            assertFalse(quiz.get("id").asInt() == quizId);
        }

        // Teacher publishes it, then it can be started.
        Result toggled = call("PATCH", "/quizzes/" + quizId + "/status", teacher, null);
        assertEquals(200, toggled.status());
        assertTrue(json(toggled).get("quiz").get("published").asBoolean());
        assertEquals(201, call("POST", "/quizzes/" + quizId + "/start", student, null).status());
    }

    @Test
    void deletingAQuizRemovesItsAttempts() throws Exception {
        String teacher = createStaffAndLogin(Role.TEACHER);
        String student = registerAndLogin(uniqueEmail("del"));
        int quizId = createQuiz(teacher, true, 10);
        Map<Integer, Integer> correct = correctOptions(quizId);

        int attemptId = json(call("POST", "/quizzes/" + quizId + "/start", student, null)).get("attemptId").asInt();
        call("POST", "/quizzes/" + quizId + "/submit", student,
                map("attemptId", attemptId, "answers", answers(correct, 2)));
        assertEquals(1, json(call("GET", "/student/results", student, null)).size());

        assertEquals(200, call("DELETE", "/quizzes/" + quizId, teacher, null).status());
        assertEquals(404, call("GET", "/quizzes/" + quizId, teacher, null).status());
        assertEquals(0, json(call("GET", "/student/results", student, null)).size());
    }

    @Test
    void invalidIdsAreBadRequests() throws Exception {
        String teacher = createStaffAndLogin(Role.TEACHER);
        assertEquals(400, call("GET", "/quizzes/abc", teacher, null).status());
        assertEquals(404, call("GET", "/quizzes/999999", teacher, null).status());
    }

    @Test
    void adminUserManagement() throws Exception {
        String admin = createStaffAndLogin(Role.ADMIN);
        String email = uniqueEmail("promote");
        registerAndLogin(email);

        JsonNode users = json(call("GET", "/admin/users", admin, null));
        int userId = -1;
        for (JsonNode user : users) {
            if (email.equals(user.get("email").asText())) {
                userId = user.get("id").asInt();
            }
        }
        assertTrue(userId > 0);
        int adminId = json(call("GET", "/me", admin, null)).get("id").asInt();

        Result promoted = call("PUT", "/admin/users/" + userId + "/role", admin, map("role", "teacher"));
        assertEquals(200, promoted.status());
        assertEquals("teacher", json(promoted).get("user").get("role").asText());

        assertEquals(400, call("PUT", "/admin/users/" + userId + "/role", admin, map("role", "wizard")).status());
        assertEquals(400, call("PUT", "/admin/users/" + adminId + "/role", admin, map("role", "student")).status());
        assertEquals(400, call("DELETE", "/admin/users/" + adminId, admin, null).status());

        assertEquals(200, call("DELETE", "/admin/users/" + userId, admin, null).status());
        assertEquals(404, call("DELETE", "/admin/users/" + userId, admin, null).status());
    }

    @Test
    void studentProfileUpdate() throws Exception {
        String student = registerAndLogin(uniqueEmail("profile"));
        Result updated = call("PUT", "/student/profile", student, map("name", "Neo", "surname", "Anderson"));
        assertEquals(200, updated.status());
        assertEquals("Neo", json(updated).get("student").get("name").asText());
        assertEquals(400, call("PUT", "/student/profile", student, map("name", "Neo")).status());
    }

    @Test
    void legacyPeopleEndpoints() throws Exception {
        String student = registerAndLogin(uniqueEmail("people"));
        assertEquals(201, call("POST", "/people", student, map("name", "Grace", "surname", "Hopper")).status());
        Result found = app.handle(new Request("GET", "/people", "Bearer " + student, Map.of("search", "hopp"), null));
        assertEquals(200, found.status());
        assertEquals(1, json(found).size());
    }
}
