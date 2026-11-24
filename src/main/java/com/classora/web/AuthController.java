package com.classora.web;

import com.classora.service.AuthService;

public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    public void register(Router router) {
        router.post("/register", Access.publicAccess(), ctx -> ApiResponse.created(
                auth.register(
                        Json.text(ctx.body(), "email"),
                        Json.text(ctx.body(), "password"),
                        Json.text(ctx.body(), "name"),
                        Json.text(ctx.body(), "surname"))));

        router.post("/login", Access.publicAccess(), ctx -> ApiResponse.ok(
                auth.login(
                        Json.text(ctx.body(), "email"),
                        Json.text(ctx.body(), "password"))));

        router.get("/me", Access.authenticated(), ctx -> ApiResponse.ok(auth.me(ctx.caller())));
    }
}
