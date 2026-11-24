package com.classora.web;

import com.classora.model.Role;
import com.classora.service.AdminService;

public class AdminController {

    private static final String INVALID_USER = "Invalid user ID";

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    public void register(Router router) {
        Access adminOnly = Access.roles(Role.ADMIN);

        router.get("/admin/users", adminOnly, ctx -> ApiResponse.ok(admin.listUsers()));

        router.put("/admin/users/{id}/role", adminOnly, ctx -> ApiResponse.ok(
                admin.changeRole(ctx.caller(), ctx.pathInt("id", INVALID_USER),
                        Json.text(ctx.body(), "role"))));

        router.delete("/admin/users/{id}", adminOnly, ctx -> ApiResponse.ok(
                admin.deleteUser(ctx.caller(), ctx.pathInt("id", INVALID_USER))));
    }
}
