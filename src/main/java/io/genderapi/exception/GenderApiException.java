package io.genderapi.exception;

/**
 * Base class of all SDK exceptions. Messages never contain the API key or submitted values.
 *
 * <ul>
 *   <li>{@link GenderApiValidationException}: invalid input, detected before any network call.</li>
 *   <li>{@link GenderApiHttpException}: the API answered with HTTP 4xx/5xx.</li>
 *   <li>{@link GenderApiTransportException}: no usable response (timeout, connection failure,
 *       redirect, malformed body). The billing outcome may be unknown.</li>
 * </ul>
 */
public class GenderApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GenderApiException(String message) {
        super(message);
    }

    public GenderApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
