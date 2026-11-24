package com.classora.web;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterTest {

    private final Router router = new Router()
            .get("/quizzes", Access.authenticated(), ctx -> ApiResponse.ok("list"))
            .get("/quizzes/{id}", Access.authenticated(), ctx -> ApiResponse.ok("one"))
            .post("/quizzes/{id}/start", Access.staff(), ctx -> ApiResponse.ok("start"))
            .get("/teacher/students", Access.staff(), ctx -> ApiResponse.ok("all"))
            .get("/teacher/students/passed", Access.staff(), ctx -> ApiResponse.ok("passed"));

    @Test
    void matchesPlainPaths() {
        assertTrue(router.find("GET", "/quizzes").isPresent());
        assertTrue(router.find("GET", "/quizzes/").isPresent());
    }

    @Test
    void capturesPathParameters() {
        Router.Match match = router.find("GET", "/quizzes/42").orElseThrow();
        assertEquals(Map.of("id", "42"), match.pathParams());
    }

    @Test
    void methodMustMatch() {
        assertTrue(router.find("POST", "/quizzes/7/start").isPresent());
        assertFalse(router.find("GET", "/quizzes/7/start").isPresent());
        assertTrue(router.pathExists("/quizzes/7/start"));
    }

    @Test
    void longerAndShorterPathsAreDifferentRoutes() {
        assertTrue(router.find("GET", "/teacher/students").isPresent());
        assertTrue(router.find("GET", "/teacher/students/passed").isPresent());
        assertFalse(router.find("GET", "/teacher/students/passed/extra").isPresent());
    }

    @Test
    void unknownPathsAreNotFound() {
        assertFalse(router.find("GET", "/nothing").isPresent());
        assertFalse(router.pathExists("/nothing"));
    }

    @Test
    void accessRulesDescribeWhoMayCall() {
        assertFalse(Access.publicAccess().loginRequired());
        assertTrue(Access.authenticated().loginRequired());
        assertTrue(Access.staff().allows(com.classora.model.Role.TEACHER));
        assertTrue(Access.staff().allows(com.classora.model.Role.ADMIN));
        assertFalse(Access.staff().allows(com.classora.model.Role.STUDENT));
        assertTrue(Access.authenticated().allows(com.classora.model.Role.STUDENT));
    }
}
