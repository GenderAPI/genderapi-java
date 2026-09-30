package io.genderapi.exception;

import com.fasterxml.jackson.databind.JsonNode;
import io.genderapi.model.JsonBacked;
import io.genderapi.model.Meta;
import io.genderapi.model.Usage;

/**
 * An API key is configured, but the successful response reports another access mode in
 * {@code meta.access.mode} (usually {@code ip_trial}, because the key was not recognized).
 *
 * <p>The request has already been processed and may have consumed shared IP-trial credits; it
 * is not retried. The complete response the method would have returned is available from
 * {@link #getResult()} (typed via {@link #getResult(Class)}) and as JSON from {@link #getJson()}.
 * Disable this check with {@code GenderApiClient.builder().requireApiKeyAccess(false)}.
 */
public final class GenderApiAccessModeException extends GenderApiException {

    private static final long serialVersionUID = 1L;

    /** Stable code of this error. */
    public static final String CODE = "unexpected_access_mode";

    private final String accessMode;
    private final String accessReason;
    private final int status;
    private final String requestId;
    private final transient JsonBacked result;

    /**
     * @param result          the complete typed response (e.g. a {@code GenderResponse})
     * @param accessMode      {@code meta.access.mode}, e.g. {@code ip_trial}
     * @param accessReason    {@code meta.access.reason}, e.g. {@code api_key_invalid}; may be {@code null}
     * @param status          the HTTP status (2xx)
     * @param headerRequestId the {@code X-Request-ID} header, used when {@code meta.request_id} is absent
     */
    public GenderApiAccessModeException(JsonBacked result, String accessMode, String accessReason,
                                        int status, String headerRequestId) {
        super("Expected API-key access but the response reports access mode "
                + (accessMode == null ? "null" : "\"" + accessMode + "\"")
                + ". Check your API key; this request may have consumed IP-trial credits.");
        this.result = result;
        this.accessMode = accessMode;
        this.accessReason = accessReason;
        this.status = status;
        this.requestId = resolveRequestId(result, headerRequestId);
    }

    private static String resolveRequestId(JsonBacked result, String header) {
        if (result != null) {
            JsonNode metaId = result.raw().path("meta").path("request_id");
            if (metaId.isTextual() && !metaId.asText().isEmpty()) {
                return metaId.asText();
            }
        }
        return header == null || header.isEmpty() ? null : header;
    }

    /** @return {@value #CODE} */
    public String getCode() {
        return CODE;
    }

    /** @return {@code meta.access.mode} as reported by the server, e.g. {@code ip_trial} */
    public String getAccessMode() {
        return accessMode;
    }

    /** @return {@code meta.access.reason}, e.g. {@code api_key_invalid} or {@code api_key_not_found}; may be {@code null} */
    public String getAccessReason() {
        return accessReason;
    }

    /** @return the HTTP status of the (successful) response */
    public int getStatus() {
        return status;
    }

    /** @return {@code meta.request_id}, else the {@code X-Request-ID} header; {@code null} if none */
    public String getRequestId() {
        return requestId;
    }

    /**
     * @return the complete response the method would have returned: a {@code GenderResponse},
     * {@code BatchResponse}, {@code UsageResponse} or {@code PhoneResponse}
     */
    public JsonBacked getResult() {
        return result;
    }

    /**
     * @param type the expected response class, e.g. {@code GenderResponse.class}
     * @param <T>  the response type
     * @return the complete response cast to {@code type}
     * @throws ClassCastException if the response is of another type
     */
    public <T extends JsonBacked> T getResult(Class<T> type) {
        return type.cast(result);
    }

    /** @return a copy of the complete parsed JSON body (may contain submitted personal data) */
    public JsonNode getJson() {
        return result == null ? null : result.raw();
    }

    /** @return the response {@code meta}, or {@code null} */
    public Meta getMeta() {
        JsonNode meta = result == null ? null : result.raw().get("meta");
        return meta != null && meta.isObject() ? new Meta(meta) : null;
    }

    /** @return {@code meta.usage} (billing outcome of this request), or {@code null} */
    public Usage getUsage() {
        Meta meta = getMeta();
        return meta == null ? null : meta.getUsage();
    }
}
