package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** One entry of the public error catalog ({@code GET /errors}). */
public final class ErrorCatalogEntry extends JsonBacked {

    public ErrorCatalogEntry(JsonNode node) {
        super(node);
    }

    public String getCode() {
        return text("code");
    }

    public List<Integer> getHttpStatuses() {
        return list(child("http_statuses"), JsonNode::asInt);
    }

    public String getAction() {
        return text("action");
    }

    public String getDescription() {
        return text("description");
    }
}
