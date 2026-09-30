package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@code meta.summary} of a batch: {@code total = succeeded + failed} and
 * {@code succeeded = identified + unknown}. Unknown is a successful, billed prediction.
 */
public final class BatchSummary extends JsonBacked {

    public BatchSummary(JsonNode node) {
        super(node);
    }

    public Integer getTotal() {
        return integer("total");
    }

    public Integer getSucceeded() {
        return integer("succeeded");
    }

    public Integer getIdentified() {
        return integer("identified");
    }

    public Integer getUnknown() {
        return integer("unknown");
    }

    public Integer getFailed() {
        return integer("failed");
    }
}
