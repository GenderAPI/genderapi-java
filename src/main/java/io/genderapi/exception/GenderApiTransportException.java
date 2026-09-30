package io.genderapi.exception;

/**
 * No usable API response was received. For prediction and phone requests the server may still
 * have completed (and billed) the operation, so the SDK never retries automatically. Check
 * {@code usage()} or contact support with {@link #getRequestId()} before sending a new request.
 */
public final class GenderApiTransportException extends GenderApiException {

    private static final long serialVersionUID = 1L;

    /** Why no usable response was available. */
    public enum Kind {
        /** The configured timeout elapsed (connect, headers or body). */
        TIMEOUT,
        /** The connection failed or was closed. */
        CONNECTION,
        /** The server answered with a 3xx redirect, which the SDK never follows. */
        REDIRECT,
        /** A 2xx response that is not a JSON object. */
        INVALID_RESPONSE,
        /** The calling thread was interrupted while waiting. */
        INTERRUPTED
    }

    private final Kind kind;
    private final Integer status;
    private final String requestId;

    public GenderApiTransportException(Kind kind, String message, Integer status, String requestId, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.status = status;
        this.requestId = requestId;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isTimeout() {
        return kind == Kind.TIMEOUT;
    }

    /** @return the HTTP status if a response line was received, otherwise {@code null} */
    public Integer getStatus() {
        return status;
    }

    /** @return the {@code X-Request-ID} header if a response was received, otherwise {@code null} */
    public String getRequestId() {
        return requestId;
    }
}
