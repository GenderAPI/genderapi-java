package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/** The input echoed back in {@code data.input}. */
public final class Input extends JsonBacked {

    public Input(JsonNode node) {
        super(node);
    }

    /** @return {@code name}, {@code email} or {@code username} */
    public String getType() {
        return text("type");
    }

    public String getValue() {
        return text("value");
    }

    /** @return the requested country, or {@code null} if none was sent */
    public String getCountry() {
        return text("country");
    }

    /** @return {@code true} when the request set forceToGenderize; {@code null} when absent */
    public Boolean getForceToGenderize() {
        return bool("forceToGenderize");
    }
}
