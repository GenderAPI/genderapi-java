package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One batch result row. Exactly one of {@link #getData()} and {@link #getError()} is present.
 * Failed items report {@code charged_credits} 0 after a confirmed refund.
 */
public final class BatchItem extends JsonBacked {

    public BatchItem(JsonNode node) {
        super(node);
    }

    /** @return the zero-based position of the item in the submitted batch */
    public Integer getIndex() {
        return integer("index");
    }

    /** @return the optional id you submitted, or {@code null} */
    public String getId() {
        return text("id");
    }

    public Integer getChargedCredits() {
        return integer("charged_credits");
    }

    /** @return the prediction for a succeeded item, otherwise {@code null} */
    public Prediction getData() {
        return object("data", Prediction::new);
    }

    /** @return the item problem for a failed item, otherwise {@code null} */
    public Problem getError() {
        return object("error", Problem::new);
    }

    public boolean isSuccess() {
        return child("data") != null && !has("error");
    }

    public boolean isFailure() {
        return has("error");
    }
}
