# Classora (Java)

Student management and online quiz platform. A Java rebuild of the original
Node.js/Express version, with the same REST API and the same front end.

Students take timed multiple-choice quizzes and see their results and ranking.
Teachers manage quizzes and view performance. Admins manage users and roles.

## Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Web | Jakarta Servlet 5 (runs on Tomcat 10.1 or Jetty 11) |
| Persistence | Hibernate ORM 6 (JPA), HikariCP pool |
| Database | PostgreSQL (H2 in PostgreSQL mode for tests) |
| Auth | JWT (HS256), BCrypt, role-based access |
| JSON | Jackson |
| Build / test | Maven, JUnit 5 |
| Front end | HTML, CSS, JavaScript (unchanged from the Node version) |

## Architecture

```
HTTP request
   |
ApiServlet            thin adapter: servlet request <-> Application.handle()
   |
Application           routing, JWT check, role check, error -> JSON
   |
Controllers           read the request, call a service   (web/)
   |
Services              business rules, transactions       (service/)
   |
DAOs                  JPQL queries                       (dao/)
   |
Entities              Hibernate mappings                 (model/)
```

- `web/Router` matches `METHOD /path/{param}` to a handler. Each route declares
  who may call it (`Access.publicAccess()`, `authenticated()`, `roles(...)`).
- `Application` has no servlet classes in it. That is why the whole API can be
  tested against H2 without a servlet container (`ApiIntegrationTest`).
- Pure rules live in small classes with no database code: `Grading`, `Ranking`,
  `QuizAvailability`, `PerformanceReport`, `DateTimes`. They have fast unit tests.
- `Database.inTransaction(...)` gives each service call one `EntityManager`
  and one transaction, and rolls back on any exception.
- Design patterns used: front controller (`ApiServlet`/`Application`), DAO,
  service layer, DTOs (`dto/Dtos`), strategy-style `Access` rules, and
  constructor injection (wired by hand in `Application`).

## Run it locally

You need Java 17+, Maven and a PostgreSQL database.

```bash
export DATABASE_URL=postgresql://user:password@localhost:5432/classora
export JWT_SECRET=a-long-random-string

mvn jetty:run          # http://localhost:8080
```

The tables are created on start-up (`src/main/resources/schema.sql`). The script
is safe to run on the existing production database: it only adds what is missing.

### On Android (Termux)

```bash
pkg install openjdk-17 maven postgresql git

mkdir -p $PREFIX/var/lib/postgresql
initdb $PREFIX/var/lib/postgresql
pg_ctl -D $PREFIX/var/lib/postgresql -l ~/pg.log start
createdb classora

export DATABASE_URL=postgresql://$(whoami)@localhost:5432/classora
export JWT_SECRET=a-long-random-string
mvn jetty:run
```

## Create a teacher or admin

Public sign-up only creates students. Create staff accounts with:

```bash
mvn -q compile exec:java -Dexec.args="admin@example.com MyPassword123 admin"
mvn -q compile exec:java -Dexec.args="teacher@example.com MyPassword123 teacher"
```

Running it again for an existing email updates that user's password and role.
An admin can also change roles from the admin portal.

## Tests

```bash
mvn test
```

- Unit tests: grading, ranking, dates, database URL parsing, quiz validation,
  availability rules, performance reports, router, JWT, password policy.
- `ApiIntegrationTest` runs the full API against an in-memory H2 database:
  register, login, role checks, quiz creation, start/resume, grading, late
  submissions, results, ranking, teacher reports, admin user management.

## Deploy

**Render (Docker):** create a Web Service from this repository. Render uses the
`Dockerfile` (Maven build, then Tomcat 10.1). Set `DATABASE_URL` and
`JWT_SECRET` in the service's environment settings.

**Any Tomcat 10.1+:** `mvn package`, then deploy `target/classora.war`.

### Environment variables

| Name | Required | Notes |
|---|---|---|
| `DATABASE_URL` | yes | `postgresql://user:pass@host:5432/db`, or a `jdbc:` URL |
| `JWT_SECRET` | yes | Use the same value as the Node app to keep existing logins valid |
| `JWT_TTL_MINUTES` | no | Token lifetime, default 60 |

Hosted databases get `sslmode=require` automatically. Localhost does not.

## API

Same paths and JSON field names as the Node version, so the front end works unchanged.

| Method and path | Who |
|---|---|
| `POST /api/register`, `POST /api/login`, `GET /api/health` | public |
| `GET /api/me` | any logged-in user |
| `GET /api/quizzes`, `GET /api/quizzes/{id}` | any logged-in user |
| `PUT /api/student/profile`, `GET /api/student/dashboard`, `/results`, `/ranking` | student |
| `POST /api/quizzes/{id}/start`, `POST /api/quizzes/{id}/submit` | student |
| `POST /api/quizzes`, `PATCH /api/quizzes/{id}/status`, `DELETE /api/quizzes/{id}` | teacher, admin |
| `GET /api/teacher/students`, `/students/passed`, `/students/failed`, `/analytics` | teacher, admin |
| `GET /api/admin/users`, `PUT /api/admin/users/{id}/role`, `DELETE /api/admin/users/{id}` | admin |
| `GET/POST /api/people` | any logged-in user (original CRUD demo) |

## How it differs from the Node version

Behaviour is the same except for these deliberate fixes:

1. **Unfinished attempts are ignored in statistics.** The Node version counted a
   started-but-unsubmitted attempt as a 0% fail in averages, results, the teacher
   lists and the ranking. Only submitted attempts count now.
2. **Submitted answers are checked.** An option that does not belong to the
   question is treated as unanswered.
3. **Duplicate sign-ups** that race each other return 409 instead of 500.
4. **Complete schema.** The Node start-up script did not create several columns
   the code used (`time_limit`, `attempts_allowed`, `start_at`, `end_at`,
   `published`, `started_at`). `schema.sql` includes them.
5. **Clearer errors.** Invalid JSON returns 400. Unexpected failures return a
   generic 500 message and are logged on the server.
6. **Password length** is limited to 72 bytes (a BCrypt limit that Node truncated silently).
7. **Quiz titles** that are blank are rejected.

## Known limitations

- **Quiz dates have no time zone.** The browser's date picker sends
  `2026-11-22T10:00` with no zone, and both versions store it as UTC. Teachers
  outside UTC will see times shifted. Fix on the front end by sending
  `new Date(value).toISOString()`; the Java side already accepts both formats.
- **Role changes and deletions take effect when the token expires** (1 hour),
  because the role is read from the token.
- Tokens are kept in `localStorage` by the front end.
