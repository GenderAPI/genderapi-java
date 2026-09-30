package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/** {@code data} of {@code GET /usage}. */
public final class UsageData extends JsonBacked {

    public UsageData(JsonNode node) {
        super(node);
    }

    /** @return the current balance; can be negative */
    public Long getRemainingCredits() {
        return longValue("remaining_credits");
    }

    /** @return the package expiry timestamp, or {@code null} */
    public String getExpiresAt() {
        return text("expires_at");
    }

    /** @return the UTC IP-trial reset timestamp, or {@code null} */
    public String getResetsAt() {
        return text("resets_at");
    }

    public Integer getLimit() {
        return integer("limit");
    }

    public Integer getPeriodSeconds() {
        return integer("period_seconds");
    }
}
