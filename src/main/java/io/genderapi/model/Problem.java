package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * RFC 9457 Problem Details as returned by the API, used for failed batch items and inside
 * {@link io.genderapi.exception.GenderApiHttpException}. Match on {@link #getCode()}, never on
 * the human-readable {@link #getDetail()}.
 */
public final class Problem extends JsonBacked {

    public Problem(JsonNode node) {
        super(node);
    }

    public String getType() {
        return text("type");
    }

    public String getTitle() {
        return text("title");
    }

    public Integer getStatus() {
        return integer("status");
    }

    public String getDetail() {
        return text("detail");
    }

    public String getInstance() {
        return text("instance");
    }

    /** @return the stable machine-readable error code, e.g. {@code ai_upstream_error} */
    public String getCode() {
        return text("code");
    }

    public String getRequestId() {
        return text("request_id");
    }

    public String getDocumentation() {
        return text("documentation");
    }

    /** @return the recommended action, e.g. {@code inspect_billing_before_retry} */
    public String getAction() {
        return text("action");
    }

    /** @return validation pointers; empty when none */
    public List<ValidationIssue> getErrors() {
        return list(child("errors"), ValidationIssue::new);
    }

    /** @return {@code meta}, present on top-level problems and absent on batch item errors */
    public Meta getMeta() {
        return object("meta", Meta::new);
    }
}
