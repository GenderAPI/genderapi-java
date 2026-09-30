package io.genderapi.exception;

/**
 * Client-side validation failed. No request was sent and nothing was charged.
 */
public final class GenderApiValidationException extends GenderApiException {

    private static final long serialVersionUID = 1L;

    private final String field;

    public GenderApiValidationException(String field, String message) {
        super(field == null ? message : field + ": " + message);
        this.field = field;
    }

    /** @return the offending field, e.g. {@code value} or {@code items[3].country}; may be {@code null} */
    public String getField() {
        return field;
    }
}
