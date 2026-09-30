package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/** {@code meta}: request reference, access and accounting. */
public final class Meta extends JsonBacked {

    public Meta(JsonNode node) {
        super(node);
    }

    /** @return the ID of this HTTP attempt (also sent as the {@code X-Request-ID} header) */
    public String getRequestId() {
        return text("request_id");
    }

    public Long getDurationMs() {
        return longValue("duration_ms");
    }

    public Access getAccess() {
        return object("access", Access::new);
    }

    public Usage getUsage() {
        return object("usage", Usage::new);
    }

    /** @return the batch summary, or {@code null} for non-batch responses */
    public BatchSummary getSummary() {
        return object("summary", BatchSummary::new);
    }
}
