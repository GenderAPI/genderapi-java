package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/** Response of {@code POST /phone/validate}. */
public final class PhoneResponse extends JsonBacked {

    public PhoneResponse(JsonNode node) {
        super(node);
    }

    public PhoneValidation getData() {
        return object("data", PhoneValidation::new);
    }

    public Meta getMeta() {
        return object("meta", Meta::new);
    }
}
