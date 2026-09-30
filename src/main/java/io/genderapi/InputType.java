package io.genderapi;

/** The {@code type} of a gender request. */
public enum InputType {
    /** A personal name. */
    NAME("name"),
    /** An email address. */
    EMAIL("email"),
    /** A username or handle. */
    USERNAME("username");

    private final String wireValue;

    InputType(String wireValue) {
        this.wireValue = wireValue;
    }

    /** @return the exact value sent in the {@code type} field */
    public String wireValue() {
        return wireValue;
    }
}
