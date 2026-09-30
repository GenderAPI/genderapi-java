package io.genderapi.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A gender inference ({@code data} of a single response, or {@code data} of a batch item).
 *
 * <p>The result is an inference from a name, email or username. It can be unknown
 * ({@code gender == null}), which is a successful, billable outcome. It never verifies a person's
 * identity, and the country fields do not establish nationality, residence or ethnicity.
 */
public final class Prediction extends JsonBacked {

    public static final String IDENTIFIED = "identified";
    public static final String UNKNOWN = "unknown";

    public Prediction(JsonNode node) {
        super(node);
    }

    public Input getInput() {
        return object("input", Input::new);
    }

    /** @return the returned dataset name or extracted given name; may be {@code null} */
    public String getName() {
        return text("name");
    }

    /** @return {@code male}, {@code female}, or {@code null} when the result is unknown */
    public String getGender() {
        return text("gender");
    }

    public String getCountry() {
        return text("country");
    }

    /**
     * The raw {@code confidence} value on a 0 to 1 scale, exactly as returned. Its meaning depends on
     * {@link #getConfidenceKind()}: {@code observed_frequency} is the dominant dataset share,
     * {@code model_reported} is a model score, not a calibrated probability. It is not a percentage
     * and not a measured accuracy.
     *
     * @return the confidence, or {@code null} when the gender is unknown
     */
    public Double getConfidence() {
        return number("confidence");
    }

    /** @return {@code observed_frequency}, {@code model_reported} or {@code null} */
    public String getConfidenceKind() {
        return text("confidence_kind");
    }

    /** @return the dataset sample count, or {@code null} (always {@code null} for AI results) */
    public Integer getSampleCount() {
        return integer("sample_count");
    }

    /** @return {@code dataset}, {@code ai} or {@code none} */
    public String getSource() {
        return text("source");
    }

    /** @return {@code identified} or {@code unknown} */
    public String getResultStatus() {
        return text("result_status");
    }

    /**
     * @return {@code null} for identified results; otherwise {@code not_found},
     * {@code no_name_candidate}, {@code ambiguous} or {@code insufficient_evidence}
     */
    public String getReason() {
        return text("reason");
    }

    /** @return {@code dataset}, {@code ai_association} or {@code null} */
    public String getCountrySource() {
        return text("country_source");
    }

    public Match getMatch() {
        return object("match", Match::new);
    }

    /** @return whether {@code result_status} is {@code identified} */
    public boolean isIdentified() {
        return IDENTIFIED.equals(getResultStatus());
    }

    /** @return whether {@code result_status} is {@code unknown} */
    public boolean isUnknown() {
        return UNKNOWN.equals(getResultStatus());
    }
}
