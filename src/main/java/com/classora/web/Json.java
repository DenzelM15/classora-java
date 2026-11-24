package com.classora.web;

import com.fasterxml.jackson.databind.JsonNode;

/** Forgiving readers for request bodies. A wrong type counts as "missing". */
public final class Json {

    private Json() {
    }

    public static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    /** Whole numbers, or text holding a whole number ("30"), like JavaScript's Number(). */
    public static Integer integer(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return integerValue(value);
    }

    public static Integer integerValue(JsonNode value) {
        if (value == null) {
            return null;
        }
        if (value.isIntegralNumber() && value.canConvertToInt()) {
            return value.intValue();
        }
        if (value.isTextual()) {
            try {
                return Integer.parseInt(value.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /** Only a real JSON true counts. */
    public static boolean isTrue(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isBoolean() && value.booleanValue();
    }
}
