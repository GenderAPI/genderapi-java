package io.genderapi;

/**
 * {@code options.ai_mode}. When omitted, the server defaults to {@code fallback} for single
 * requests and {@code off} for batch items.
 *
 * <ul>
 *   <li>{@link #OFF}: dataset only (1 credit).</li>
 *   <li>{@link #FALLBACK}: dataset first, AI only when needed (1 credit total).</li>
 *   <li>{@link #ALWAYS}: explicit AI inference (2 credits).</li>
 * </ul>
 * Tariffs are product rules; the response's {@code meta.usage} is authoritative.
 */
public enum AiMode {
    OFF("off"),
    FALLBACK("fallback"),
    ALWAYS("always");

    private final String wireValue;

    AiMode(String wireValue) {
        this.wireValue = wireValue;
    }

    /** @return the exact value sent in {@code options.ai_mode} */
    public String wireValue() {
        return wireValue;
    }
}
