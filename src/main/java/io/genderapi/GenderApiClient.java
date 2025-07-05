package io.genderapi;

import io.genderapi.model.GenderApiResult;
import io.genderapi.model.GenderErrorResponse;
import io.genderapi.model.GenderSuccessResponse;

import com.fasterxml.jackson.databind.JsonNode;

import io.genderapi.exception.GenderApiException;
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

/**
 * Java SDK for GenderAPI.io
 *
 * This SDK allows determining gender from:
 *   - personal names
 *   - email addresses
 *   - social media usernames
 *
 * It supports additional options like:
 *   - country filtering
 *   - direct AI queries
 *   - forced genderization for nicknames or unconventional strings
 *
 * Usage Example:
 * <pre>
 *     GenderApiClient client = new GenderApiClient("YOUR_API_KEY");
 *     GenderApiResult result = client.getGenderByName("Michael", "US", false, false);
 *     System.out.println(result.getGender());
 * </pre>
 */
public class GenderApiClient {

    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Create a GenderAPI client with the default base URL.
     *
     * @param apiKey Your API key for GenderAPI.io
     */
    public GenderApiClient(String apiKey) {
        this(apiKey, "https://api.genderapi.io");
    }

    /**
     * Create a GenderAPI client with a custom base URL.
     *
     * @param apiKey  Your API key for GenderAPI.io
     * @param baseUrl Base URL for the API endpoint
     */
    public GenderApiClient(String apiKey, String baseUrl) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API key must not be null or empty.");
        }

        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Determine gender from a personal name.
     *
     * @param name                Name to analyze. (Required)
     * @param country             Optional two-letter country code (e.g. "US") to improve accuracy.
     * @param askToAI             Whether to directly query AI (costs extra credits).
     * @param forceToGenderize    Whether to analyze nicknames, emojis, or non-standard names.
     * @return GenderApiResult object containing the API result.
     * @throws GenderApiException if an HTTP or parsing error occurs.
     */
    public GenderApiResult getGenderByName(String name, String country, boolean askToAI, boolean forceToGenderize)
            throws GenderApiException {

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name parameter is required.");
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("name", name);
        if (country != null) payload.put("country", country);
        payload.put("askToAI", askToAI);
        payload.put("forceToGenderize", forceToGenderize);

        return sendPost("/api", payload);
    }

    /**
     * Determine gender from an email address.
     *
     * @param email     Email to analyze. (Required)
     * @param country   Optional two-letter country code.
     * @param askToAI   Whether to directly query AI.
     * @return GenderApiResult object containing the API result.
     * @throws GenderApiException if an HTTP or parsing error occurs.
     */
    public GenderApiResult getGenderByEmail(String email, String country, boolean askToAI)
            throws GenderApiException {

        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email parameter is required.");
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("email", email);
        if (country != null) payload.put("country", country);
        payload.put("askToAI", askToAI);

        return sendPost("/api/email", payload);
    }

    /**
     * Determine gender from a social media username.
     *
     * @param username            Username to analyze. (Required)
     * @param country             Optional two-letter country code.
     * @param askToAI             Whether to directly query AI.
     * @param forceToGenderize    Whether to analyze nicknames, emojis, etc.
     * @return GenderApiResult object containing the API result.
     * @throws GenderApiException if an HTTP or parsing error occurs.
     */
    public GenderApiResult getGenderByUsername(String username, String country, boolean askToAI, boolean forceToGenderize)
            throws GenderApiException {

        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username parameter is required.");
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("username", username);
        if (country != null) payload.put("country", country);
        payload.put("askToAI", askToAI);
        payload.put("forceToGenderize", forceToGenderize);

        return sendPost("/api/username", payload);
    }

    /**
     * Internal method to send a POST request to GenderAPI.
     *
     * - Handles authentication
     * - Cleans up payload JSON
     * - Parses JSON response
     * - Maps HTTP errors into GenderApiException
     *
     * @param endpoint API endpoint path (e.g. "/api")
     * @param payload  JSON payload for the POST request.
     * @return GenderApiResult object parsed from JSON.
     * @throws GenderApiException if a network or API error occurs.
     */
    private GenderApiResult sendPost(String endpoint, ObjectNode payload) throws GenderApiException {
        String url = baseUrl + endpoint;

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(
                        payload.toString(),
                        MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {

            String json = response.body() != null ? response.body().string() : "";

            // Ağ hatası varsa exception fırlat
            if (response.code() == 500 ||
                    response.code() == 502 ||
                    response.code() == 503 ||
                    response.code() == 504 ||
                    response.code() == 408) {
                throw new GenderApiException("Server error (" + response.code() + ")");
            }

            // JSON parse et
            JsonNode jsonNode = objectMapper.readTree(json);

            if (!jsonNode.has("status")) {
                throw new GenderApiException("Invalid API response: Missing 'status' field.");
            }

            boolean status = jsonNode.get("status").asBoolean();

            if (status) {
                // Başarılı durum
                return objectMapper.treeToValue(jsonNode, GenderSuccessResponse.class);
            } else {
                // Hatalı durum
                return objectMapper.treeToValue(jsonNode, GenderErrorResponse.class);
            }

        } catch (IOException e) {
            throw new GenderApiException("Request failed: " + e.getMessage(), e);
        }
    }


}
