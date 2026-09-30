package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/** Response of the free {@code GET /usage} endpoint. */
public final class UsageResponse extends JsonBacked {

    public UsageResponse(JsonNode node) {
        super(node);
    }

    public UsageData getData() {
        return object("data", UsageData::new);
    }

    public Meta getMeta() {
        return object("meta", Meta::new);
    }
}
