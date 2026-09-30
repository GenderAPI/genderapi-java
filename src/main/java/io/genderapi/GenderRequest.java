package io.genderapi;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.genderapi.exception.GenderApiValidationException;

import java.util.Objects;

/**
 * An immutable gender request, used for single calls and as a batch item. Each {@code with}-style
 * method returns a new instance.
 *
 * <pre>{@code
 * GenderRequest request = GenderRequest.name("Andrea").country("IT").aiMode(AiMode.OFF).id("row-1");
 * }</pre>
 *
 * Serialized with the exact wire fields {@code id}, {@code type}, {@code value}, {@code country},
 * {@code forceToGenderize} and {@code options.ai_mode}. Unset optional fields are omitted.
 */
public final class GenderRequest {

    private final InputType type;
    private final String value;
    private final String country;
    private final AiMode aiMode;
    private final boolean forceToGenderize;
    private final String id;

    private GenderRequest(InputType type, String value, String country, AiMode aiMode,
                          boolean forceToGenderize, String id) {
        this.type = type;
        this.value = value;
        this.country = country;
        this.aiMode = aiMode;
        this.forceToGenderize = forceToGenderize;
        this.id = id;
    }

    /**
     * @param type  name, email or username
     * @param value the value to analyze (1-254 characters)
     * @return a new request; it is validated when sent (or by {@link #validate()})
     */
    public static GenderRequest of(InputType type, String value) {
        return new GenderRequest(type, value, null, null, false, null);
    }

    public static GenderRequest name(String value) {
        return of(InputType.NAME, value);
    }

    public static GenderRequest email(String value) {
        return of(InputType.EMAIL, value);
    }

    public static GenderRequest username(String value) {
        return of(InputType.USERNAME, value);
    }

    /**
     * @param country optional uppercase ISO 3166-1 alpha-2 code, or {@code null} to omit
     * @return a copy with the country set
     */
    public GenderRequest country(String country) {
        return new GenderRequest(type, value, country, aiMode, forceToGenderize, id);
    }

    /**
     * @param aiMode {@code options.ai_mode}, or {@code null} for the server default
     * @return a copy with the AI mode set
     */
    public GenderRequest aiMode(AiMode aiMode) {
        return new GenderRequest(type, value, country, aiMode, forceToGenderize, id);
    }

    /**
     * Dataset first (1 credit); on an unknown gender, nickname-aware AI (2 credits total). Cannot be
     * combined with {@link AiMode#OFF} or {@link AiMode#ALWAYS}.
     *
     * @param forceToGenderize whether to send {@code forceToGenderize: true}
     * @return a copy with the flag set
     */
    public GenderRequest forceToGenderize(boolean forceToGenderize) {
        return new GenderRequest(type, value, country, aiMode, forceToGenderize, id);
    }

    /**
     * @param id optional caller reference (1-64 characters), echoed in batch results
     * @return a copy with the id set
     */
    public GenderRequest id(String id) {
        return new GenderRequest(type, value, country, aiMode, forceToGenderize, id);
    }

    public InputType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getCountry() {
        return country;
    }

    public AiMode getAiMode() {
        return aiMode;
    }

    public boolean isForceToGenderize() {
        return forceToGenderize;
    }

    public String getId() {
        return id;
    }

    /**
     * Runs the client-side checks without sending anything.
     *
     * @throws GenderApiValidationException if the request is invalid
     */
    public void validate() {
        validate("");
    }

    void validate(String prefix) {
        if (type == null) {
            throw new GenderApiValidationException(prefix + "type", "type must be name, email or username");
        }
        Validation.value(prefix + "value", value);
        Validation.country(prefix + "country", country);
        Validation.id(prefix + "id", id);
        if (forceToGenderize && (aiMode == AiMode.OFF || aiMode == AiMode.ALWAYS)) {
            throw new GenderApiValidationException(prefix + "forceToGenderize",
                    "forceToGenderize cannot be combined with ai_mode off or always");
        }
    }

    ObjectNode toJson() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        if (id != null) {
            node.put("id", id);
        }
        node.put("type", type.wireValue());
        node.put("value", value);
        if (country != null) {
            node.put("country", country);
        }
        if (forceToGenderize) {
            node.put("forceToGenderize", true);
        }
        if (aiMode != null) {
            node.putObject("options").put("ai_mode", aiMode.wireValue());
        }
        return node;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GenderRequest)) {
            return false;
        }
        GenderRequest that = (GenderRequest) other;
        return forceToGenderize == that.forceToGenderize && type == that.type && Objects.equals(value, that.value)
                && Objects.equals(country, that.country) && aiMode == that.aiMode && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value, country, aiMode, forceToGenderize, id);
    }

    /** Omits the value, which can be personal data. */
    @Override
    public String toString() {
        return "GenderRequest{type=" + type + ", country=" + country + ", aiMode=" + aiMode
                + ", forceToGenderize=" + forceToGenderize + ", id=" + id + "}";
    }
}
