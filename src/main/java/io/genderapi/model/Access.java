package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/** {@code meta.access}: how the request was authorized. */
public final class Access extends JsonBacked {

    public static final String API_KEY = "api_key";
    public static final String IP_TRIAL = "ip_trial";
    public static final String UNAUTHENTICATED = "unauthenticated";

    public Access(JsonNode node) {
        super(node);
    }

    /** @return {@code api_key}, {@code ip_trial} or {@code unauthenticated} */
    public String getMode() {
        return text("mode");
    }

    /**
     * @return {@code null}, or for trials {@code api_key_missing}, {@code api_key_invalid} or
     * {@code api_key_not_found}
     */
    public String getReason() {
        return text("reason");
    }

    /** @return whether the request used your API key (and not the shared IP trial) */
    public boolean isApiKey() {
        return API_KEY.equals(getMode());
    }

    /** @return whether the request used the shared IP trial */
    public boolean isIpTrial() {
        return IP_TRIAL.equals(getMode());
    }
}
