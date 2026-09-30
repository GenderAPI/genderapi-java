package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/** {@code data} of {@code POST /phone/validate}. */
public final class PhoneValidation extends JsonBacked {

    public PhoneValidation(JsonNode node) {
        super(node);
    }

    public Boolean getValid() {
        return bool("valid");
    }

    public Boolean getPossible() {
        return bool("possible");
    }

    public String getE164() {
        return text("e164");
    }

    public String getCountry() {
        return text("country");
    }

    public Integer getCountryCallingCode() {
        return integer("country_calling_code");
    }
}
