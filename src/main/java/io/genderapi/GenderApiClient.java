package io.genderapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.genderapi.exception.GenderApiException;
import io.genderapi.exception.GenderApiHttpException;
import io.genderapi.exception.GenderApiTransportException;
import io.genderapi.exception.GenderApiValidationException;
import io.genderapi.model.BatchResponse;
import io.genderapi.model.ErrorCatalog;
import io.genderapi.model.GenderResponse;
import io.genderapi.model.PhoneResponse;
import io.genderapi.model.UsageResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Official GenderAPI.io V2 client for Java (server-side use only).
 *
 * <pre>{@code
 * GenderApiClient client = new GenderApiClient();          // reads GENDERAPI_API_KEY
 * GenderResponse result = client.name("Andrea", "IT");
 * String gender = result.getData().getGender();           // "male", "female" or null
 * }</pre>
 *
 * <p>Safety behaviour:
 * <ul>
 *   <li>No automatic retries, including on 429: a lost response may still have been billed.</li>
 *   <li>Redirects are never followed; a 3xx raises {@link GenderApiTransportException}.</li>
 *   <li>A single timeout (default 10 s) bounds the whole call.</li>
 *   <li>HTTPS only, except {@code http://localhost}, {@code 127.0.0.1} or {@code [::1]} for tests.</li>
 *   <li>The key is sent only as {@code Authorization: Bearer}, never in a URL, and never logged.</li>
 *   <li>Constructing the client makes no request.</li>
 * </ul>
 *
 * <p>Without an API key the server applies the shared IP trial (10 credits per 24 hours); check
 * {@code meta.access.mode}. Instances are immutable and thread-safe; reuse one per application.
 */
public final class GenderApiClient {

    /** SDK version, also sent in the User-Agent. */
    public static final String VERSION = "2.0.0";
    /** Production base URL. */
    public static final String DEFAULT_BASE_URL = "https://api.genderapi.io/api/v2";
    /** Default timeout for a whole call. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    /** Environment variable read when no key is passed explicitly. */
    public static final String API_KEY_ENV = "GENDERAPI_API_KEY";
    /** Maximum items per batch accepted by the API (IP trials allow 10; the server decides). */
    public static final int MAX_BATCH_ITEMS = 50;

    private static final String USER_AGENT = "genderapi-java/" + VERSION;
    private static final Set<String> LOCAL_HOSTS =
            new HashSet<>(Arrays.asList("localhost", "127.0.0.1", "[::1]", "::1"));

    private final String apiKey;
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    /** Creates a client using {@code GENDERAPI_API_KEY} if set, otherwise the IP trial. */
    public GenderApiClient() {
        this(builder());
    }

    /**
     * @param apiKey your API key; {@code null} or blank falls back to {@code GENDERAPI_API_KEY}
     */
    public GenderApiClient(String apiKey) {
        this(builder().apiKey(apiKey));
    }

    private GenderApiClient(Builder builder) {
        String key = builder.apiKey;
        if (isBlank(key) && !builder.withoutApiKey) {
            key = builder.environment.apply(API_KEY_ENV);
        }
        this.apiKey = builder.withoutApiKey || isBlank(key) ? null : checkApiKey(key.trim());
        this.baseUrl = checkBaseUrl(builder.baseUrl);
        this.timeout = builder.timeout;
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    // ------------------------------------------------------------------ gender

    /**
     * {@code POST /gender}: one billable attempt. An unknown result is a successful response.
     *
     * @param request the request
     * @return the full response, including {@code meta}
     * @throws GenderApiValidationException before any network call if the request is invalid
     * @throws GenderApiHttpException       on HTTP 4xx/5xx
     * @throws GenderApiTransportException  on timeout, connection failure, redirect or malformed body
     */
    public GenderResponse gender(GenderRequest request) {
        if (request == null) {
            throw new GenderApiValidationException("request", "request is required");
        }
        request.validate();
        return new GenderResponse(send("/gender", request.toJson(), true));
    }

    /**
     * @param type  name, email or username
     * @param value the value
     * @return the response
     */
    public GenderResponse gender(InputType type, String value) {
        return gender(GenderRequest.of(type, value));
    }

    /**
     * Full-parameter form mirroring {@code gender(type, value, country?, aiMode?, forceToGenderize?, id?)}.
     *
     * @param type             name, email or username
     * @param value            the value
     * @param country          optional ISO alpha-2 code, or {@code null}
     * @param aiMode           optional AI mode, or {@code null} for the server default
     * @param forceToGenderize whether to send {@code forceToGenderize: true}
     * @param id               optional reference, or {@code null}
     * @return the response
     */
    public GenderResponse gender(InputType type, String value, String country, AiMode aiMode,
                                 boolean forceToGenderize, String id) {
        return gender(GenderRequest.of(type, value).country(country).aiMode(aiMode)
                .forceToGenderize(forceToGenderize).id(id));
    }

    public GenderResponse name(String name) {
        return gender(GenderRequest.name(name));
    }

    public GenderResponse name(String name, String country) {
        return gender(GenderRequest.name(name).country(country));
    }

    public GenderResponse email(String email) {
        return gender(GenderRequest.email(email));
    }

    public GenderResponse email(String email, String country) {
        return gender(GenderRequest.email(email).country(country));
    }

    public GenderResponse username(String username) {
        return gender(GenderRequest.username(username));
    }

    public GenderResponse username(String username, String country) {
        return gender(GenderRequest.username(username).country(country));
    }

    // ------------------------------------------------------------------ batch

    /**
     * {@code POST /gender/batch} with 1-50 items (IP trials: 10; the server decides). Batch items
     * default to {@code ai_mode off} on the server.
     *
     * <p>Partial success returns normally: inspect {@link BatchResponse#getFailedItems()}. When every
     * executed item fails, the API answers with an error status and a {@link GenderApiHttpException}
     * is thrown whose {@link GenderApiHttpException#getBatchItems()} holds the rows.
     *
     * @param items the items; ids, when present, must be unique
     * @return the batch response
     */
    public BatchResponse genderBatch(List<GenderRequest> items) {
        if (items == null || items.isEmpty() || items.size() > MAX_BATCH_ITEMS) {
            throw new GenderApiValidationException("items",
                    "provide 1-" + MAX_BATCH_ITEMS + " items per batch; split larger jobs yourself");
        }
        Set<String> ids = new HashSet<>();
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        ArrayNode array = body.putArray("items");
        for (int i = 0; i < items.size(); i++) {
            GenderRequest item = items.get(i);
            String prefix = "items[" + i + "].";
            if (item == null) {
                throw new GenderApiValidationException("items[" + i + "]", "item is required");
            }
            item.validate(prefix);
            if (item.getId() != null && !ids.add(item.getId())) {
                throw new GenderApiValidationException(prefix + "id", "batch item ids must be unique");
            }
            array.add(item.toJson());
        }
        return new BatchResponse(send("/gender/batch", body, true));
    }

    public BatchResponse genderBatch(GenderRequest... items) {
        return genderBatch(items == null ? null : Arrays.asList(items));
    }

    // ------------------------------------------------------------------ other endpoints

    /** @return {@code GET /usage}: the free balance and quota read */
    public UsageResponse usage() {
        return new UsageResponse(send("/usage", null, true));
    }

    /**
     * {@code POST /phone/validate} (billable).
     *
     * @param number phone number, 3-32 characters
     * @return the response
     */
    public PhoneResponse validatePhone(String number) {
        return validatePhone(number, null);
    }

    /**
     * @param number  phone number, 3-32 characters
     * @param country optional ISO alpha-2 code, or {@code null}
     * @return the response
     */
    public PhoneResponse validatePhone(String number, String country) {
        Validation.phone("number", number);
        Validation.country("country", country);
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.put("number", number);
        if (country != null) {
            body.put("country", country);
        }
        return new PhoneResponse(send("/phone/validate", body, true));
    }

    /** @return {@code GET /}: deployment version, limits and AI availability (sent without credentials) */
    public JsonNode capabilities() {
        return send("", null, false);
    }

    /** @return {@code GET /errors}: the public error catalog (sent without credentials) */
    public ErrorCatalog errorCatalog() {
        return new ErrorCatalog(send("/errors", null, false));
    }

    // ------------------------------------------------------------------ accessors

    /** @return whether an API key is configured (otherwise requests use the IP trial) */
    public boolean hasApiKey() {
        return apiKey != null;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public String toString() {
        return "GenderApiClient{baseUrl=" + baseUrl + ", apiKey=" + (apiKey == null ? "<none>" : "<redacted>")
                + ", timeout=" + timeout + "}";
    }

    // ------------------------------------------------------------------ transport

    private JsonNode send(String path, ObjectNode body, boolean authenticate) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT);
        if (authenticate && apiKey != null) {
            request.header("Authorization", "Bearer " + apiKey);
        }
        if (body != null) {
            byte[] bytes;
            try {
                bytes = mapper.writeValueAsBytes(body);
            } catch (JsonProcessingException e) {
                throw new GenderApiException("Could not serialize the request body.", e);
            }
            request.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bytes));
        } else {
            request.GET();
        }

        CompletableFuture<HttpResponse<byte[]>> future =
                http.sendAsync(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        HttpResponse<byte[]> response;
        try {
            response = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw timeout(null);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new GenderApiTransportException(GenderApiTransportException.Kind.INTERRUPTED,
                    "Interrupted while waiting for GenderAPI; the billing outcome is unknown. Do not retry automatically.",
                    null, null, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof HttpTimeoutException) {
                throw timeout(cause);
            }
            throw new GenderApiTransportException(GenderApiTransportException.Kind.CONNECTION,
                    "Connection to GenderAPI failed" + (cause instanceof IOException ? "" : " (" + cause.getClass().getSimpleName() + ")")
                            + "; the billing outcome may be unknown. Do not retry automatically.",
                    null, null, cause);
        }
        return handle(response);
    }

    private static GenderApiTransportException timeout(Throwable cause) {
        return new GenderApiTransportException(GenderApiTransportException.Kind.TIMEOUT,
                "GenderAPI request timed out; the request may still have completed and been billed. "
                        + "Check usage() before sending a new request.", null, null, cause);
    }

    private JsonNode handle(HttpResponse<byte[]> response) {
        int status = response.statusCode();
        String headerRequestId = response.headers().firstValue("X-Request-ID").orElse(null);
        String retryAfter = response.headers().firstValue("Retry-After").orElse(null);

        if (status >= 300 && status < 400) {
            throw new GenderApiTransportException(GenderApiTransportException.Kind.REDIRECT,
                    "GenderAPI answered with HTTP " + status + " redirect; redirects are never followed "
                            + "so the key is not forwarded. Check the base URL.", status, headerRequestId, null);
        }

        byte[] bytes = response.body() == null ? new byte[0] : response.body();
        String raw = new String(bytes, StandardCharsets.UTF_8);
        String contentType = response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT);
        JsonNode json = null;
        if (contentType.contains("json")) {
            try {
                json = mapper.readTree(bytes);
            } catch (IOException e) {
                json = null;
            }
        }

        if (status >= 400) {
            throw new GenderApiHttpException(status, json, raw, headerRequestId, retryAfter);
        }
        if (status < 200 || json == null || !json.isObject()) {
            throw new GenderApiTransportException(GenderApiTransportException.Kind.INVALID_RESPONSE,
                    "GenderAPI returned HTTP " + status + " without a JSON object; the billing outcome may be unknown. "
                            + "Keep the request ID and check usage() before retrying.", status, headerRequestId, null);
        }
        return json;
    }

    // ------------------------------------------------------------------ configuration checks

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String checkApiKey(String key) {
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c <= 0x20 || c >= 0x7f) {
                // Never include the key itself in the message.
                throw new GenderApiValidationException("apiKey", "API key contains invalid characters");
            }
        }
        return key;
    }

    private static String checkBaseUrl(String value) {
        if (isBlank(value)) {
            throw new GenderApiValidationException("baseUrl", "base URL is required");
        }
        URI uri;
        try {
            uri = new URI(value.trim());
        } catch (URISyntaxException e) {
            throw new GenderApiValidationException("baseUrl", "base URL is not a valid URI");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (host.isEmpty() || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new GenderApiValidationException("baseUrl", "base URL must be an absolute URL without credentials, query or fragment");
        }
        boolean allowed = "https".equals(scheme) || ("http".equals(scheme) && LOCAL_HOSTS.contains(host));
        if (!allowed) {
            throw new GenderApiValidationException("baseUrl",
                    "base URL must use https (plain http is allowed only for localhost/127.0.0.1/[::1] in tests)");
        }
        String normalized = uri.toString();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    // ------------------------------------------------------------------ builder

    /** Builder for {@link GenderApiClient}. */
    public static final class Builder {

        private String apiKey;
        private boolean withoutApiKey;
        private String baseUrl = DEFAULT_BASE_URL;
        private Duration timeout = DEFAULT_TIMEOUT;
        private Function<String, String> environment = System::getenv;

        private Builder() {
        }

        /**
         * @param apiKey your API key; {@code null} or blank falls back to {@code GENDERAPI_API_KEY}
         * @return this builder
         */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            this.withoutApiKey = false;
            return this;
        }

        /**
         * Sends no key and ignores {@code GENDERAPI_API_KEY}; the server applies the shared IP trial.
         *
         * @return this builder
         */
        public Builder withoutApiKey() {
            this.apiKey = null;
            this.withoutApiKey = true;
            return this;
        }

        /**
         * Overrides the base URL (for tests). Must be https, or http on localhost/127.0.0.1/[::1].
         *
         * @param baseUrl e.g. {@code https://api.genderapi.io/api/v2}
         * @return this builder
         */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /**
         * @param timeout positive timeout for a whole call (connect, send and read); default 10 s
         * @return this builder
         */
        public Builder timeout(Duration timeout) {
            Objects.requireNonNull(timeout, "timeout");
            if (timeout.isZero() || timeout.isNegative()) {
                throw new GenderApiValidationException("timeout", "timeout must be positive");
            }
            this.timeout = timeout;
            return this;
        }

        /** Test hook: replaces the environment lookup. */
        Builder environment(Function<String, String> environment) {
            this.environment = Objects.requireNonNull(environment, "environment");
            return this;
        }

        /** @return a client; no request is made */
        public GenderApiClient build() {
            return new GenderApiClient(this);
        }
    }
}
