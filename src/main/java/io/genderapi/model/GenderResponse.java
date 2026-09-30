package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/** Response of {@code POST /gender}: {@code {data, meta}}. */
public final class GenderResponse extends JsonBacked {

    public GenderResponse(JsonNode node) {
        super(node);
    }

    public Prediction getData() {
        return object("data", Prediction::new);
    }

    public Meta getMeta() {
        return object("meta", Meta::new);
    }
}
