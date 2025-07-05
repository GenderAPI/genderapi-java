package io.genderapi.exception;

/**
 * Custom exception for errors encountered while calling the GenderAPI.io service.
 */
public class GenderApiException extends Exception {

    /**
     * Constructs a new GenderApiException with a detail message.
     *
     * @param message the detail message.
     */
    public GenderApiException(String message) {
        super(message);
    }

    /**
     * Constructs a new GenderApiException with a detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause (which is saved for later retrieval).
     */
    public GenderApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
