package com.classora.web;

import com.classora.web.Application.Request;
import com.classora.web.Application.Result;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Thin adapter: servlet request in, Application.handle(), JSON out. */
@WebServlet(name = "api", urlPatterns = "/api/*")
public class ApiServlet extends HttpServlet {

    private static final int MAX_BODY_BYTES = 1_000_000;

    // Overriding service() (not doGet/doPost) so PATCH works too.
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Application application = (Application) getServletContext().getAttribute(AppListener.ATTRIBUTE);

        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        resp.setHeader("Cache-Control", "no-store");
        resp.setHeader("X-Content-Type-Options", "nosniff");

        byte[] raw = req.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (raw.length > MAX_BODY_BYTES) {
            resp.setStatus(413);
            resp.getWriter().write("{\"error\":\"Request body is too large\"}");
            return;
        }

        Map<String, String> query = new HashMap<>();
        req.getParameterMap().forEach((name, values) -> {
            if (values.length > 0) {
                query.put(name, values[0]);
            }
        });

        Result result = application.handle(new Request(
                req.getMethod(),
                req.getPathInfo(),
                req.getHeader("Authorization"),
                query,
                new String(raw, StandardCharsets.UTF_8)));

        resp.setStatus(result.status());
        resp.getWriter().write(result.json());
    }
}
