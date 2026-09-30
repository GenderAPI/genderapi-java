package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Base class for response objects. Each object wraps the parsed JSON it was built from, so every
 * field the API returns stays available through {@link #raw()} and {@link #get(String)}, including
 * fields added after this SDK version. Typed getters return {@code null} when a field is absent or
 * JSON {@code null}; they never convert or rescale values.
 *
 * <p>{@link #toString()} returns the JSON, which can contain the submitted input (names, emails,
 * usernames). Treat it as personal data and do not log it wholesale.
 */
public abstract class JsonBacked {

    private final JsonNode node;

    protected JsonBacked(JsonNode node) {
        this.node = node == null || node.isMissingNode() ? NullNode.getInstance() : node;
    }

    /** @return a copy of the complete JSON value, including unknown fields. */
    public JsonNode raw() {
        return node.deepCopy();
    }

    /**
     * @param field JSON field name
     * @return a copy of the field's JSON value, or {@code null} when the field is absent
     */
    public JsonNode get(String field) {
        JsonNode value = node.get(field);
        return value == null ? null : value.deepCopy();
    }

    /**
     * @param field JSON field name
     * @return whether the field is present (even if its value is JSON {@code null})
     */
    public boolean has(String field) {
        return node.has(field);
    }

    protected final JsonNode node() {
        return node;
    }

    protected final JsonNode child(String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value;
    }

    protected final String text(String field) {
        JsonNode value = child(field);
        return value == null ? null : value.asText();
    }

    protected final Integer integer(String field) {
        JsonNode value = child(field);
        return value != null && value.isIntegralNumber() && value.canConvertToInt() ? value.intValue() : null;
    }

    protected final Long longValue(String field) {
        JsonNode value = child(field);
        return value != null && value.isIntegralNumber() && value.canConvertToLong() ? value.longValue() : null;
    }

    protected final Double number(String field) {
        JsonNode value = child(field);
        return value != null && value.isNumber() ? value.doubleValue() : null;
    }

    protected final Boolean bool(String field) {
        JsonNode value = child(field);
        return value != null && value.isBoolean() ? value.booleanValue() : null;
    }

    protected final <T> T object(String field, Function<JsonNode, T> factory) {
        JsonNode value = child(field);
        return value != null && value.isObject() ? factory.apply(value) : null;
    }

    protected static <T> List<T> list(JsonNode array, Function<JsonNode, T> factory) {
        if (array == null || !array.isArray()) {
            return Collections.emptyList();
        }
        List<T> out = new ArrayList<>(array.size());
        for (JsonNode element : array) {
            out.add(factory.apply(element));
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public boolean equals(Object other) {
        return other != null && other.getClass() == getClass() && node.equals(((JsonBacked) other).node);
    }

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    @Override
    public String toString() {
        return node.toString();
    }
}
