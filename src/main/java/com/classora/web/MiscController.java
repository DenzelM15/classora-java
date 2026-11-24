package com.classora.web;

import com.classora.dto.Dtos.Health;
import com.classora.service.PersonService;

/** The legacy people CRUD and the health check. */
public class MiscController {

    private final PersonService people;

    public MiscController(PersonService people) {
        this.people = people;
    }

    public void register(Router router) {
        router.post("/people", Access.authenticated(), ctx -> ApiResponse.created(
                people.save(Json.text(ctx.body(), "name"), Json.text(ctx.body(), "surname"))));

        router.get("/people", Access.authenticated(),
                ctx -> ApiResponse.ok(people.search(ctx.query("search"))));

        router.get("/health", Access.publicAccess(), ctx -> ApiResponse.ok(
                new Health("OK", "Full-stack application API is running")));
    }
}
