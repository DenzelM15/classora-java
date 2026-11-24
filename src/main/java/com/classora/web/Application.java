package com.classora.web;

import com.classora.config.AppConfig;
import com.classora.config.Database;
import com.classora.config.DatabaseUrl;
import com.classora.dao.AttemptDao;
import com.classora.dao.PersonDao;
import com.classora.dao.QuizDao;
import com.classora.dao.ReportDao;
import com.classora.dao.StudentDao;
import com.classora.dao.UserDao;
import com.classora.dto.Dtos.ErrorBody;
import com.classora.security.AuthenticatedUser;
import com.classora.security.InvalidTokenException;
import com.classora.security.JwtService;
import com.classora.security.PasswordHasher;
import com.classora.service.AdminService;
import com.classora.service.ApiException;
import com.classora.service.AttemptService;
import com.classora.service.AuthService;
import com.classora.service.PersonService;
import com.classora.service.QuizService;
import com.classora.service.ReportService;
import com.classora.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The whole API without any servlet classes: wires the layers together and turns
 * a request (method, path, token, body) into a response (status, JSON).
 * The servlet is a thin adapter around handle(), which keeps this testable.
 */
public final class Application implements AutoCloseable {

    /** A request, already stripped down to what the API needs. */
    public record Request(String method, String path, String authorization,
                          Map<String, String> query, String body) { }

    public record Result(int status, String json) { }

    private static final Logger LOG = Logger.getLogger(Application.class.getName());
    private static final String BEARER = "Bearer ";

    private final Database db;
    private final JwtService jwt;
    private final Router router = new Router();
    private final ObjectMapper mapper = new ObjectMapper();

    public Application(Database db, JwtService jwt, PasswordHasher hasher, Clock clock) {
        this.db = db;
        this.jwt = jwt;

        UserDao userDao = new UserDao();
        StudentDao studentDao = new StudentDao();
        QuizDao quizDao = new QuizDao();
        AttemptDao attemptDao = new AttemptDao();
        ReportDao reportDao = new ReportDao();
        PersonDao personDao = new PersonDao();

        AuthService authService = new AuthService(db, userDao, studentDao, hasher, jwt);
        StudentService studentService = new StudentService(db, studentDao, quizDao, attemptDao);
        QuizService quizService = new QuizService(db, quizDao, attemptDao, studentDao, clock);
        AttemptService attemptService = new AttemptService(db, quizDao, attemptDao, studentDao, clock);
        ReportService reportService = new ReportService(db, reportDao);
        AdminService adminService = new AdminService(db, userDao, studentDao);
        PersonService personService = new PersonService(db, personDao);

        new AuthController(authService).register(router);
        new StudentController(studentService, reportService).register(router);
        new QuizController(quizService, attemptService).register(router);
        new TeacherController(reportService).register(router);
        new AdminController(adminService).register(router);
        new MiscController(personService).register(router);
    }

    /** Production wiring from environment variables. */
    public static Application fromEnvironment() {
        AppConfig config = AppConfig.fromEnvironment();
        Database db = Database.connect(DatabaseUrl.parse(config.databaseUrl()));
        JwtService jwt = new JwtService(config.jwtSecret(), config.tokenLifetime(), Clock.systemUTC());
        return new Application(db, jwt, new PasswordHasher(), Clock.systemUTC());
    }

    public Result handle(Request request) {
        try {
            Optional<Router.Match> match = router.find(request.method(), request.path());

            if (match.isEmpty()) {
                if (router.pathExists(request.path())) {
                    return error(405, "Method not allowed");
                }
                return error(404, "Not found");
            }

            Route route = match.get().route();
            AuthenticatedUser caller = null;

            if (route.access().loginRequired()) {
                caller = authenticate(request.authorization());
                if (!route.access().allows(caller.role())) {
                    throw ApiException.forbidden("Access denied");
                }
            }

            RequestContext ctx = new RequestContext(match.get().pathParams(),
                    request.query() == null ? Map.of() : request.query(),
                    request.body(), caller, mapper);

            ApiResponse response = route.handler().handle(ctx);
            return new Result(response.status(), mapper.writeValueAsString(response.body()));

        } catch (ApiException e) {
            return error(e.status(), e.getMessage());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unhandled error for " + request.method() + " " + request.path(), e);
            return error(500, "Something went wrong on the server");
        }
    }

    private AuthenticatedUser authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)
                || authorization.length() == BEARER.length()) {
            throw ApiException.unauthorized("Access token required");
        }
        try {
            return jwt.verify(authorization.substring(BEARER.length()).trim());
        } catch (InvalidTokenException e) {
            throw ApiException.forbidden("Invalid or expired token");
        }
    }

    private Result error(int status, String message) {
        try {
            return new Result(status, mapper.writeValueAsString(new ErrorBody(message)));
        } catch (Exception e) {
            return new Result(status, "{\"error\":\"Error\"}");
        }
    }

    @Override
    public void close() {
        db.close();
    }
}
