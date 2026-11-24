package com.classora.web;

import com.classora.security.AuthenticatedUser;
import com.classora.service.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.Map;

/** Everything a handler needs to know about the current request. */
public final class RequestContext {

    private final Map<String, String> pathParams;
    private final Map<String, String> query;
    private final String rawBody;
    private final AuthenticatedUser caller;
    private final ObjectMapper mapper;
    private JsonNode parsedBody;

    RequestContext(Map<String, String> pathParams, Map<String, String> query, String rawBody,
                   AuthenticatedUser caller, ObjectMapper mapper) {
        this.pathParams = pathParams;
        this.query = query;
        this.rawBody = rawBody;
        this.caller = caller;
        this.mapper = mapper;
    }

    /** The logged-in user. Only available on routes that require login. */
    public AuthenticatedUser caller() {
        return caller;
    }

    /** A path parameter that must be a whole number, e.g. the 5 in /quizzes/5. */
    public int pathInt(String name, String errorMessage) {
        String value = pathParams.get(name);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest(errorMessage);
        }
    }

    public String query(String name) {
        return query.get(name);
    }

    /** The JSON body, or an empty object when there is none. */
    public JsonNode body() {
        if (parsedBody == null) {
            if (rawBody == null || rawBody.isBlank()) {
                parsedBody = JsonNodeFactory.instance.objectNode();
            } else {
                try {
                    parsedBody = mapper.readTree(rawBody);
                } catch (Exception e) {
                    throw ApiException.badRequest("Request body is not valid JSON");
                }
            }
        }
        return parsedBody;
    }
}
