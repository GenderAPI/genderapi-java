package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@code data.match}: the dataset candidate and lookup scope. For AI results and for no match,
 * {@code name} and {@code scope} are {@code null}.
 */
public final class Match extends JsonBacked {

    public Match(JsonNode node) {
        super(node);
    }

    /** @return the normalized dataset candidate that matched (not the full input), or {@code null} */
    public String getName() {
        return text("name");
    }

    /** @return {@code normalized}, {@code token}, {@code substring}, {@code model_inference} or {@code null} */
    public String getMethod() {
        return text("method");
    }

    /** @return {@code country}, {@code global} or {@code null} */
    public String getScope() {
        return text("scope");
    }

    /** @return the requested country when a country-specific dataset row was used, otherwise {@code null} */
    public String getCountry() {
        return text("country");
    }
}
