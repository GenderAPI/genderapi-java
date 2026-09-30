package io.genderapi;

import io.genderapi.exception.GenderApiValidationException;

import java.util.regex.Pattern;

/** Cheap, certain client-side checks that mirror the OpenAPI schema. The API stays authoritative. */
final class Validation {

    static final int MAX_VALUE_LENGTH = 254;
    static final int MAX_ID_LENGTH = 64;

    private static final Pattern CONTROL = Pattern.compile("[\\x00-\\x1f\\x7f]");
    private static final Pattern NON_SPACE = Pattern.compile("\\S");
    private static final Pattern COUNTRY = Pattern.compile("[A-Z]{2}");
    private static final Pattern PHONE = Pattern.compile("\\+?[0-9 ()-]+");

    private Validation() {
    }

    static void value(String field, String value) {
        if (value == null) {
            throw new GenderApiValidationException(field, "value is required");
        }
        int length = value.codePointCount(0, value.length());
        if (length < 1 || length > MAX_VALUE_LENGTH || !NON_SPACE.matcher(value).find()) {
            throw new GenderApiValidationException(field, "value must contain 1-254 characters and not be blank");
        }
        if (CONTROL.matcher(value).find()) {
            throw new GenderApiValidationException(field, "value must not contain control characters");
        }
    }

    static void country(String field, String country) {
        if (country != null && !COUNTRY.matcher(country).matches()) {
            throw new GenderApiValidationException(field,
                    "country must be an uppercase ISO 3166-1 alpha-2 code (e.g. US); omit it when unknown");
        }
    }

    static void id(String field, String id) {
        if (id != null) {
            int length = id.codePointCount(0, id.length());
            if (length < 1 || length > MAX_ID_LENGTH) {
                throw new GenderApiValidationException(field, "id must contain 1-64 characters");
            }
        }
    }

    static void phone(String field, String number) {
        if (number == null || number.length() < 3 || number.length() > 32 || !PHONE.matcher(number).matches()) {
            throw new GenderApiValidationException(field,
                    "number must be 3-32 characters of digits, spaces, parentheses, hyphens and an optional leading +");
        }
    }
}
