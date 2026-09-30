package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response of {@code POST /gender/batch} with HTTP 200. Partial success is not an exception:
 * inspect {@link #getFailedItems()}. Retry only failed items, and only once billing is confirmed;
 * resubmitting succeeded items charges them again.
 */
public final class BatchResponse extends JsonBacked {

    public BatchResponse(JsonNode node) {
        super(node);
    }

    /** @return every result row in submission order */
    public List<BatchItem> getItems() {
        return list(child("data"), BatchItem::new);
    }

    public List<BatchItem> getSucceededItems() {
        return Collections.unmodifiableList(
                getItems().stream().filter(BatchItem::isSuccess).collect(Collectors.toList()));
    }

    public List<BatchItem> getFailedItems() {
        return Collections.unmodifiableList(
                getItems().stream().filter(BatchItem::isFailure).collect(Collectors.toList()));
    }

    public boolean hasFailures() {
        return getItems().stream().anyMatch(BatchItem::isFailure);
    }

    public Meta getMeta() {
        return object("meta", Meta::new);
    }

    /** @return {@code meta.summary}, or {@code null} if absent */
    public BatchSummary getSummary() {
        Meta meta = getMeta();
        return meta == null ? null : meta.getSummary();
    }
}
