package com.classora.web;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * Matches "METHOD /path" to a route. Patterns use {name} for path parameters,
 * e.g. "/quizzes/{id}/start". No servlet classes here, so it is easy to test.
 */
public class Router {

    public record Match(Route route, Map<String, String> pathParams) { }

    private final List<Route> routes = new ArrayList<>();

    public Router add(String method, String pattern, Access access, Handler handler) {
        routes.add(new Route(method, split(pattern), access, handler));
        return this;
    }

    public Router get(String pattern, Access access, Handler handler) {
        return add("GET", pattern, access, handler);
    }

    public Router post(String pattern, Access access, Handler handler) {
        return add("POST", pattern, access, handler);
    }

    public Router put(String pattern, Access access, Handler handler) {
        return add("PUT", pattern, access, handler);
    }

    public Router patch(String pattern, Access access, Handler handler) {
        return add("PATCH", pattern, access, handler);
    }

    public Router delete(String pattern, Access access, Handler handler) {
        return add("DELETE", pattern, access, handler);
    }

    public Optional<Match> find(String method, String path) {
        List<String> segments = split(path);
        for (Route route : routes) {
            if (route.method().equalsIgnoreCase(method)) {
                Map<String, String> params = matchSegments(route.segments(), segments);
                if (params != null) {
                    return Optional.of(new Match(route, params));
                }
            }
        }
        return Optional.empty();
    }

    /** True when some route has this path, whatever the method (used for 405 vs 404). */
    public boolean pathExists(String path) {
        List<String> segments = split(path);
        return routes.stream().anyMatch(r -> matchSegments(r.segments(), segments) != null);
    }

    private static Map<String, String> matchSegments(List<String> pattern, List<String> actual) {
        if (pattern.size() != actual.size()) {
            return null;
        }
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < pattern.size(); i++) {
            String expected = pattern.get(i);
            String value = actual.get(i);
            if (expected.startsWith("{") && expected.endsWith("}")) {
                params.put(expected.substring(1, expected.length() - 1), value);
            } else if (!expected.equals(value)) {
                return null;
            }
        }
        return params;
    }

    private static List<String> split(String path) {
        if (path == null) {
            return List.of();
        }
        return Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toList();
    }
}
