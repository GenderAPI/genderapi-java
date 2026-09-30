package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/** One entry of a Problem's {@code errors} array: a JSON pointer into the request and a message. */
public final class ValidationIssue extends JsonBacked {

    public ValidationIssue(JsonNode node) {
        super(node);
    }

    /** @return a JSON pointer such as {@code /value} or {@code /items/2/country} */
    public String getPointer() {
        return text("pointer");
    }

    public String getMessage() {
        return text("message");
    }
}
