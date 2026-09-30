package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@code meta.usage}: the accounting for this HTTP attempt.
 *
 * <table>
 *   <caption>billing_status</caption>
 *   <tr><th>billing_status</th><th>charged_credits</th><th>meaning</th></tr>
 *   <tr><td>not_charged</td><td>0</td><td>rejected before debit, or an unbilled read</td></tr>
 *   <tr><td>confirmed</td><td>integer &gt;= 0</td><td>the net charge is known</td></tr>
 *   <tr><td>unconfirmed</td><td>null</td><td>the outcome cannot be confirmed; do not retry automatically</td></tr>
 * </table>
 */
public final class Usage extends JsonBacked {

    public static final String NOT_CHARGED = "not_charged";
    public static final String CONFIRMED = "confirmed";
    public static final String UNCONFIRMED = "unconfirmed";

    public Usage(JsonNode node) {
        super(node);
    }

    /** @return the net charge, or {@code null} when billing is unconfirmed */
    public Integer getChargedCredits() {
        return integer("charged_credits");
    }

    /**
     * @return the balance at completion (can be negative), or {@code null} when unknown. It is not a
     * promise about the balance after other concurrent requests.
     */
    public Long getRemainingCredits() {
        return longValue("remaining_credits");
    }

    /** @return {@code not_charged}, {@code confirmed} or {@code unconfirmed} */
    public String getBillingStatus() {
        return text("billing_status");
    }

    /** @return the UTC IP-trial reset timestamp, or {@code null}; not a subscription expiry */
    public String getResetsAt() {
        return text("resets_at");
    }

    /** @return the IP-trial credit limit, or {@code null} */
    public Integer getLimit() {
        return integer("limit");
    }

    /** @return the IP-trial period in seconds, or {@code null} */
    public Integer getPeriodSeconds() {
        return integer("period_seconds");
    }

    public boolean isConfirmed() {
        return CONFIRMED.equals(getBillingStatus());
    }

    public boolean isUnconfirmed() {
        return UNCONFIRMED.equals(getBillingStatus());
    }

    public boolean isNotCharged() {
        return NOT_CHARGED.equals(getBillingStatus());
    }
}
