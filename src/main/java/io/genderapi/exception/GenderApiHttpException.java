package io.genderapi.exception;

import com.fasterxml.jackson.databind.JsonNode;
import io.genderapi.model.BatchItem;
import io.genderapi.model.Meta;
import io.genderapi.model.Problem;
import io.genderapi.model.Usage;
import io.genderapi.model.ValidationIssue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The API answered with HTTP 400 or above. The SDK never retries: inspect {@link #getCode()},
 * {@link #getAction()} and {@link #getBillingStatus()} first.
 *
 * <ul>
 *   <li>429: wait for {@link #getRetryAfter()}; a new request is a new, billable operation.</li>
 *   <li>{@code billing_status = unconfirmed}: contact support with {@link #getRequestId()} before
 *       retrying.</li>
 *   <li>An all-failed batch keeps its per-item rows in {@link #getBatchItems()}.</li>
 * </ul>
 *
 * <p>Proxy-level failures can return a non-JSON body; then only {@link #getStatus()},
 * {@link #getRequestId()}, {@link #getRetryAfter()} and {@link #getRawBody()} are available.
 * The raw body can contain submitted personal data; do not log it wholesale.
 */
public final class GenderApiHttpException extends GenderApiException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String requestId;
    private final String retryAfter;
    private final String rawBody;
    private final transient JsonNode json;

    public GenderApiHttpException(int status, JsonNode json, String rawBody, String headerRequestId, String retryAfter) {
        super(buildMessage(status, json));
        this.status = status;
        this.json = json != null && json.isObject() ? json : null;
        this.rawBody = rawBody;
        this.retryAfter = retryAfter;
        this.requestId = resolveRequestId(this.json, headerRequestId);
    }

    private static String buildMessage(int status, JsonNode json) {
        String code = json != null && json.isObject() && json.path("code").isTextual() ? json.get("code").asText() : null;
        StringBuilder message = new StringBuilder("GenderAPI returned HTTP ").append(status);
        if (code != null) {
            message.append(" (").append(code).append(')');
        }
        message.append(". Inspect code, action and billing status before sending another request; the SDK does not retry.");
        return message.toString();
    }

    private static String resolveRequestId(JsonNode json, String header) {
        if (json != null) {
            JsonNode metaId = json.path("meta").path("request_id");
            if (metaId.isTextual() && !metaId.asText().isEmpty()) {
                return metaId.asText();
            }
            JsonNode bodyId = json.path("request_id");
            if (bodyId.isTextual() && !bodyId.asText().isEmpty()) {
                return bodyId.asText();
            }
        }
        return header == null || header.isEmpty() ? null : header;
    }

    /** @return the HTTP status code */
    public int getStatus() {
        return status;
    }

    /** @return the parsed Problem, or {@code null} if the body was not a JSON object */
    public Problem getProblem() {
        return json == null ? null : new Problem(json);
    }

    /** @return the stable error code (e.g. {@code insufficient_credits}), or {@code null} */
    public String getCode() {
        Problem problem = getProblem();
        return problem == null ? null : problem.getCode();
    }

    public String getTitle() {
        Problem problem = getProblem();
        return problem == null ? null : problem.getTitle();
    }

    public String getDetail() {
        Problem problem = getProblem();
        return problem == null ? null : problem.getDetail();
    }

    public String getType() {
        Problem problem = getProblem();
        return problem == null ? null : problem.getType();
    }

    public String getInstance() {
        Problem problem = getProblem();
        return problem == null ? null : problem.getInstance();
    }

    public String getDocumentation() {
        Problem problem = getProblem();
        return problem == null ? null : problem.getDocumentation();
    }

    /** @return the recommended action (e.g. {@code wait_then_retry}), or {@code null} */
    public String getAction() {
        Problem problem = getProblem();
        return problem == null ? null : problem.getAction();
    }

    /** @return validation pointers from {@code errors}; empty when none */
    public List<ValidationIssue> getErrors() {
        Problem problem = getProblem();
        return problem == null ? Collections.<ValidationIssue>emptyList() : problem.getErrors();
    }

    /**
     * @return {@code meta.request_id}, else the body's {@code request_id}, else the
     * {@code X-Request-ID} header; {@code null} if none
     */
    public String getRequestId() {
        return requestId;
    }

    /** @return the raw {@code Retry-After} header value, or {@code null} */
    public String getRetryAfter() {
        return retryAfter;
    }

    /** @return {@code Retry-After} as seconds when it is a non-negative integer, otherwise {@code null} */
    public Long getRetryAfterSeconds() {
        if (retryAfter == null) {
            return null;
        }
        try {
            long seconds = Long.parseLong(retryAfter.trim());
            return seconds >= 0 ? seconds : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Meta getMeta() {
        Problem problem = getProblem();
        return problem == null ? null : problem.getMeta();
    }

    public Usage getUsage() {
        Meta meta = getMeta();
        return meta == null ? null : meta.getUsage();
    }

    /** @return {@code meta.usage.billing_status} ({@code not_charged}, {@code confirmed}, {@code unconfirmed}) or {@code null} */
    public String getBillingStatus() {
        Usage usage = getUsage();
        return usage == null ? null : usage.getBillingStatus();
    }

    public Integer getChargedCredits() {
        Usage usage = getUsage();
        return usage == null ? null : usage.getChargedCredits();
    }

    public Long getRemainingCredits() {
        Usage usage = getUsage();
        return usage == null ? null : usage.getRemainingCredits();
    }

    /** @return whether billing is explicitly {@code unconfirmed}; contact support before retrying */
    public boolean isBillingUnconfirmed() {
        return Usage.UNCONFIRMED.equals(getBillingStatus());
    }

    /**
     * @return the per-item rows of an all-failed batch ({@code data}, or the {@code results}
     * alias); empty for other errors
     */
    public List<BatchItem> getBatchItems() {
        if (json == null) {
            return Collections.emptyList();
        }
        JsonNode rows = json.path("data").isArray() ? json.get("data") : json.path("results");
        if (!rows.isArray()) {
            return Collections.emptyList();
        }
        List<BatchItem> out = new ArrayList<>(rows.size());
        for (JsonNode row : rows) {
            out.add(new BatchItem(row));
        }
        return Collections.unmodifiableList(out);
    }

    /** @return a copy of the parsed JSON body, or {@code null} if it was not a JSON object */
    public JsonNode getJson() {
        return json == null ? null : json.deepCopy();
    }

    /** @return the raw response body (may contain personal data) */
    public String getRawBody() {
        return rawBody;
    }
}
