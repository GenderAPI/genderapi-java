package io.genderapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/** Loads fixtures straight from the bundled openapi-v2.json examples so tests track the contract. */
final class Contract {

    static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNode OPENAPI = load("/contract/openapi-v2.json");
    private static final JsonNode ERROR_CATALOG = load("/contract/error-catalog.json");

    private Contract() {
    }

    private static JsonNode load(String resource) {
        try (InputStream in = Contract.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("missing test resource " + resource);
            }
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** @return the example value for e.g. ("/api/v2/gender", "post", "200", "dataset") */
    static ObjectNode example(String path, String method, String status, String name) {
        JsonNode content = OPENAPI.path("paths").path(path).path(method).path("responses").path(status).path("content");
        JsonNode media = content.has("application/json") ? content.get("application/json") : content.path("application/problem+json");
        JsonNode value = media.path("examples").path(name).path("value");
        if (!value.isObject()) {
            throw new IllegalStateException("no example " + name + " for " + method + " " + path + " " + status);
        }
        return (ObjectNode) value.deepCopy();
    }

    static String json(JsonNode node) {
        return node.toString();
    }

    /** @return {"errors": <error-catalog.json>} as served by GET /api/v2/errors */
    static String errorCatalogBody() {
        ObjectNode root = MAPPER.createObjectNode();
        root.set("errors", ERROR_CATALOG.deepCopy());
        return root.toString();
    }

    /** A Problem body shaped like the contract examples, for statuses without a published example. */
    static ObjectNode problem(int status, String code, String action, String billingStatus, Integer charged) {
        ObjectNode base = example("/api/v2/gender", "post", "403", "insufficient");
        base.put("type", "urn:genderapi:problem:" + code);
        base.put("title", code.replace('_', ' '));
        base.put("status", status);
        base.put("detail", "Synthetic " + code + " problem.");
        base.put("code", code);
        base.put("action", action);
        ObjectNode usage = (ObjectNode) base.path("meta").path("usage");
        usage.put("billing_status", billingStatus);
        if (charged == null) {
            usage.putNull("charged_credits");
            usage.putNull("remaining_credits");
        } else {
            usage.put("charged_credits", charged);
        }
        return base;
    }
}
