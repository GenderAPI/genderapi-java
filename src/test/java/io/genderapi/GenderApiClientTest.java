package io.genderapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.genderapi.StubServer.Reply;
import io.genderapi.exception.GenderApiException;
import io.genderapi.exception.GenderApiHttpException;
import io.genderapi.exception.GenderApiTransportException;
import io.genderapi.exception.GenderApiValidationException;
import io.genderapi.model.BatchItem;
import io.genderapi.model.BatchResponse;
import io.genderapi.model.ErrorCatalog;
import io.genderapi.model.GenderResponse;
import io.genderapi.model.PhoneResponse;
import io.genderapi.model.Prediction;
import io.genderapi.model.UsageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenderApiClientTest {

    private static final String KEY = "0123456789abcdef01234567"; // synthetic test key

    private StubServer stub;
    private GenderApiClient client;

    @BeforeEach
    void setUp() throws Exception {
        stub = new StubServer();
        client = clientBuilder().apiKey(KEY).build();
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    private GenderApiClient.Builder clientBuilder() {
        return GenderApiClient.builder().baseUrl(stub.baseUrl()).environment(name -> null);
    }

    private void respondExample(String path, String status, String name) {
        stub.respond(Reply.json(Integer.parseInt(status), Contract.json(Contract.example(path, "post", status, name))));
    }

    private JsonNode sentBody() throws Exception {
        return Contract.MAPPER.readTree(stub.only().body);
    }

    // ------------------------------------------------------------------ single success

    @Test
    void datasetResultIsParsedWithoutConvertingConfidence() throws Exception {
        respondExample("/api/v2/gender", "200", "dataset");

        GenderResponse response = client.gender(GenderRequest.name("Onur").country("TR").aiMode(AiMode.OFF));

        Prediction data = response.getData();
        assertEquals("male", data.getGender());
        assertEquals("onur", data.getName());
        assertEquals("TR", data.getCountry());
        assertEquals(0.9, data.getConfidence());
        assertEquals("observed_frequency", data.getConfidenceKind());
        assertEquals(100, data.getSampleCount());
        assertEquals("dataset", data.getSource());
        assertEquals("identified", data.getResultStatus());
        assertTrue(data.isIdentified());
        assertNull(data.getReason());
        assertEquals("dataset", data.getCountrySource());
        assertEquals("normalized", data.getMatch().getMethod());
        assertEquals("country", data.getMatch().getScope());
        assertEquals("Onur", data.getInput().getValue());

        assertEquals("11111111-1111-4111-8111-111111111111", response.getMeta().getRequestId());
        assertEquals("ip_trial", response.getMeta().getAccess().getMode());
        assertEquals("api_key_missing", response.getMeta().getAccess().getReason());
        assertEquals("confirmed", response.getMeta().getUsage().getBillingStatus());
        assertEquals(1, response.getMeta().getUsage().getChargedCredits());
        assertEquals(9L, response.getMeta().getUsage().getRemainingCredits());
        assertEquals("2026-09-26T12:00:00.000Z", response.getMeta().getUsage().getResetsAt());
        assertEquals(10, response.getMeta().getUsage().getLimit());
        assertEquals(86400, response.getMeta().getUsage().getPeriodSeconds());

        StubServer.Recorded request = stub.only();
        assertEquals("POST", request.method);
        assertEquals("/api/v2/gender", request.path);
        assertEquals(Contract.MAPPER.readTree(
                "{\"type\":\"name\",\"value\":\"Onur\",\"country\":\"TR\",\"options\":{\"ai_mode\":\"off\"}}"), sentBody());
    }

    @Test
    void aiAliasResultKeepsNullSampleCountAndNegativeBalance() throws Exception {
        respondExample("/api/v2/gender", "200", "alias");

        GenderResponse response = client.gender(
                GenderRequest.username("prenses").country("TR").forceToGenderize(true));

        Prediction data = response.getData();
        assertEquals("female", data.getGender());
        assertNull(data.getName());
        assertEquals(0.7, data.getConfidence());
        assertEquals("model_reported", data.getConfidenceKind());
        assertNull(data.getSampleCount());
        assertEquals("ai", data.getSource());
        assertEquals("ai_association", data.getCountrySource());
        assertEquals("model_inference", data.getMatch().getMethod());
        assertNull(data.getMatch().getScope());
        assertEquals(Boolean.TRUE, data.getInput().getForceToGenderize());
        assertEquals(2, response.getMeta().getUsage().getChargedCredits());
        assertEquals(-1L, response.getMeta().getUsage().getRemainingCredits());

        assertEquals(Contract.MAPPER.readTree(
                "{\"type\":\"username\",\"value\":\"prenses\",\"country\":\"TR\",\"forceToGenderize\":true}"), sentBody());
    }

    @Test
    void unknownResultIsASuccessfulResponse() {
        respondExample("/api/v2/gender", "200", "unknown");

        GenderResponse response = client.name("zzzxxyy");

        Prediction data = response.getData();
        assertTrue(data.isUnknown());
        assertFalse(data.isIdentified());
        assertNull(data.getGender());
        assertNull(data.getConfidence());
        assertNull(data.getConfidenceKind());
        assertEquals("not_found", data.getReason());
        assertEquals("none", data.getSource());
        assertTrue(data.has("gender"));
        assertTrue(data.raw().get("gender").isNull());
        assertEquals(1, response.getMeta().getUsage().getChargedCredits());
    }

    @Test
    void convenienceMethodsSendExactWireFields() throws Exception {
        respondExample("/api/v2/gender", "200", "dataset");

        client.email("alex@example.com", "US");
        client.username("alex_dev");
        client.gender(InputType.NAME, "Alex", "GB", AiMode.ALWAYS, false, "row-1");
        client.gender(InputType.NAME, "Alex", null, AiMode.FALLBACK, true, null);

        List<StubServer.Recorded> requests = stub.requests();
        assertEquals(4, requests.size());
        assertEquals(Contract.MAPPER.readTree("{\"type\":\"email\",\"value\":\"alex@example.com\",\"country\":\"US\"}"),
                Contract.MAPPER.readTree(requests.get(0).body));
        assertEquals(Contract.MAPPER.readTree("{\"type\":\"username\",\"value\":\"alex_dev\"}"),
                Contract.MAPPER.readTree(requests.get(1).body));
        assertEquals(Contract.MAPPER.readTree(
                        "{\"id\":\"row-1\",\"type\":\"name\",\"value\":\"Alex\",\"country\":\"GB\",\"options\":{\"ai_mode\":\"always\"}}"),
                Contract.MAPPER.readTree(requests.get(2).body));
        assertEquals(Contract.MAPPER.readTree(
                        "{\"type\":\"name\",\"value\":\"Alex\",\"forceToGenderize\":true,\"options\":{\"ai_mode\":\"fallback\"}}"),
                Contract.MAPPER.readTree(requests.get(3).body));
    }

    @Test
    void unknownFieldsAreToleratedAndKept() {
        ObjectNode body = Contract.example("/api/v2/gender", "post", "200", "dataset");
        body.put("future_top", 1);
        ((ObjectNode) body.get("data")).put("future_field", "x");
        ((ObjectNode) body.get("data").get("match")).put("distance", 0);
        ((ObjectNode) body.get("meta")).putObject("future_meta").put("a", true);
        ((ObjectNode) body.get("meta").get("usage")).put("currency", "credits");
        stub.respond(Reply.json(200, body.toString()));

        GenderResponse response = client.name("Onur");

        assertEquals("male", response.getData().getGender());
        assertEquals("x", response.getData().get("future_field").asText());
        assertEquals(0, response.getData().getMatch().get("distance").asInt());
        assertEquals("credits", response.getMeta().getUsage().get("currency").asText());
        assertTrue(response.getMeta().has("future_meta"));
        assertEquals(1, response.raw().get("future_top").asInt());
    }

    // ------------------------------------------------------------------ headers and keys

    @Test
    void sendsBearerKeyAndHeadersButNeverTheKeyInTheUrl() {
        respondExample("/api/v2/gender", "200", "dataset");

        client.name("Onur");

        StubServer.Recorded request = stub.only();
        assertEquals("Bearer " + KEY, request.header("Authorization"));
        assertEquals("application/json", request.header("Content-Type"));
        assertEquals("application/json", request.header("Accept"));
        assertEquals("genderapi-java/2.0.0", request.header("User-Agent"));
        assertNull(request.query);
        assertFalse(request.path.contains(KEY));
        assertFalse(request.body.contains(KEY));
    }

    @Test
    void withoutKeySendsNoAuthorizationHeaderForTheIpTrial() {
        respondExample("/api/v2/gender", "200", "dataset");
        GenderApiClient trial = clientBuilder().build();

        assertFalse(trial.hasApiKey());
        GenderResponse response = trial.name("Onur");

        assertNull(stub.only().header("Authorization"));
        assertEquals("ip_trial", response.getMeta().getAccess().getMode());
    }

    @Test
    void readsKeyFromEnvironmentUnlessDisabled() {
        respondExample("/api/v2/gender", "200", "dataset");
        GenderApiClient fromEnv = clientBuilder()
                .environment(name -> GenderApiClient.API_KEY_ENV.equals(name) ? KEY : null).build();
        GenderApiClient ignoringEnv = clientBuilder()
                .environment(name -> KEY).withoutApiKey().build();

        fromEnv.name("Onur");
        ignoringEnv.name("Onur");

        assertEquals("Bearer " + KEY, stub.requests().get(0).header("Authorization"));
        assertNull(stub.requests().get(1).header("Authorization"));
    }

    @Test
    void keyIsNeverExposedInToStringOrErrors() {
        stub.respond(Reply.json(401, Contract.problem(401, "invalid_api_key", "check_credentials", "not_charged", 0).toString()));

        assertFalse(client.toString().contains(KEY));
        GenderApiHttpException error = assertThrows(GenderApiHttpException.class, () -> client.name("Onur"));
        assertFalse(error.getMessage().contains(KEY));
        GenderApiValidationException invalid = assertThrows(GenderApiValidationException.class,
                () -> GenderApiClient.builder().apiKey("bad key\n" + KEY).build());
        assertFalse(invalid.getMessage().contains(KEY));
    }

    // ------------------------------------------------------------------ batch

    @Test
    void partialBatchSuccessReturnsNormallyAndExposesFailedItems() throws Exception {
        stub.respond(Reply.json(200, Contract.json(Contract.example("/api/v2/gender/batch", "post", "200", "batch"))));

        BatchResponse response = client.genderBatch(
                GenderRequest.name("Onur").country("TR").id("known"),
                GenderRequest.name("zzzxxyy").id("missing"),
                GenderRequest.name("Alex").aiMode(AiMode.ALWAYS).id("failed"));

        assertEquals(3, response.getItems().size());
        assertTrue(response.hasFailures());
        assertEquals(2, response.getSucceededItems().size());
        List<BatchItem> failed = response.getFailedItems();
        assertEquals(1, failed.size());
        BatchItem item = failed.get(0);
        assertEquals(2, item.getIndex());
        assertEquals("failed", item.getId());
        assertEquals(0, item.getChargedCredits());
        assertNull(item.getData());
        assertEquals("ai_upstream_error", item.getError().getCode());
        assertEquals("inspect_billing_before_retry", item.getError().getAction());

        BatchItem unknown = response.getItems().get(1);
        assertTrue(unknown.isSuccess());
        assertTrue(unknown.getData().isUnknown());
        assertEquals(1, unknown.getChargedCredits());

        assertEquals(3, response.getSummary().getTotal());
        assertEquals(2, response.getSummary().getSucceeded());
        assertEquals(1, response.getSummary().getIdentified());
        assertEquals(1, response.getSummary().getUnknown());
        assertEquals(1, response.getSummary().getFailed());
        assertEquals(2, response.getMeta().getUsage().getChargedCredits());

        StubServer.Recorded request = stub.only();
        assertEquals("/api/v2/gender/batch", request.path);
        assertEquals(Contract.MAPPER.readTree("{\"items\":["
                + "{\"id\":\"known\",\"type\":\"name\",\"value\":\"Onur\",\"country\":\"TR\"},"
                + "{\"id\":\"missing\",\"type\":\"name\",\"value\":\"zzzxxyy\"},"
                + "{\"id\":\"failed\",\"type\":\"name\",\"value\":\"Alex\",\"options\":{\"ai_mode\":\"always\"}}]}"), sentBody());
    }

    @Test
    void allFailedBatchThrowsAndKeepsItemRows() {
        stub.respond(Reply.json(502, Contract.json(Contract.example("/api/v2/gender/batch", "post", "502", "batchFailed"))));

        GenderApiHttpException error = assertThrows(GenderApiHttpException.class,
                () -> client.genderBatch(Collections.singletonList(GenderRequest.name("Alex").aiMode(AiMode.ALWAYS))));

        assertEquals(502, error.getStatus());
        assertEquals("ai_upstream_error", error.getCode());
        assertEquals("inspect_billing_before_retry", error.getAction());
        assertEquals("confirmed", error.getBillingStatus());
        assertEquals(0, error.getChargedCredits());
        assertEquals(1, error.getBatchItems().size());
        assertEquals("ai_upstream_error", error.getBatchItems().get(0).getError().getCode());
        assertEquals(1, error.getMeta().getSummary().getFailed());
        assertEquals(1, stub.requests().size());
    }

    // ------------------------------------------------------------------ HTTP errors, no retries

    @Test
    void unauthorizedIsStructured() {
        stub.respond(Reply.json(401, Contract.problem(401, "invalid_api_key", "check_credentials", "not_charged", 0).toString()));

        GenderApiHttpException error = assertThrows(GenderApiHttpException.class, () -> client.name("Onur"));

        assertEquals(401, error.getStatus());
        assertEquals("invalid_api_key", error.getCode());
        assertEquals("check_credentials", error.getAction());
        assertEquals("not_charged", error.getBillingStatus());
        assertEquals(1, stub.requests().size());
    }

    @Test
    void insufficientCreditsExposesProblemFields() {
        respondExample("/api/v2/gender", "403", "insufficient");

        GenderApiHttpException error = assertThrows(GenderApiHttpException.class, () -> client.name("Onur"));

        assertEquals(403, error.getStatus());
        assertEquals("insufficient_credits", error.getCode());
        assertEquals("insufficient credits", error.getTitle());
        assertEquals("At least one starting credit is required.", error.getDetail());
        assertEquals("add_credits_or_wait_for_reset", error.getAction());
        assertEquals("urn:genderapi:problem:insufficient_credits", error.getType());
        assertEquals("https://api.genderapi.io/api/v2/errors", error.getDocumentation());
        assertEquals("11111111-1111-4111-8111-111111111111", error.getRequestId());
        assertEquals(0L, error.getRemainingCredits());
        assertNotNull(error.getRawBody());
        assertEquals(1, stub.requests().size());
    }

    @Test
    void validationProblemExposesPointers() {
        respondExample("/api/v2/gender", "422", "validation");

        GenderApiHttpException error = assertThrows(GenderApiHttpException.class, () -> client.email("not-an-email"));

        assertEquals(422, error.getStatus());
        assertEquals("validation_error", error.getCode());
        assertEquals(1, error.getErrors().size());
        assertEquals("/value", error.getErrors().get(0).getPointer());
        assertEquals("Invalid email address.", error.getErrors().get(0).getMessage());
        assertNull(error.getRemainingCredits());
    }

    @Test
    void rateLimitIsNotRetriedAndSurfacesRetryAfter() {
        stub.respond(Reply.json(429, Contract.problem(429, "rate_limit_exceeded", "wait_then_retry", "not_charged", 0).toString())
                .header("Retry-After", "30").header("X-Request-ID", "hdr-id"));

        GenderApiHttpException error = assertThrows(GenderApiHttpException.class, () -> client.name("Onur"));

        assertEquals(429, error.getStatus());
        assertEquals("rate_limit_exceeded", error.getCode());
        assertEquals("30", error.getRetryAfter());
        assertEquals(30L, error.getRetryAfterSeconds());
        assertEquals("11111111-1111-4111-8111-111111111111", error.getRequestId(), "meta.request_id wins over the header");
        assertEquals(1, stub.requests().size(), "no automatic retry on 429");
    }

    @Test
    void retryAfterHttpDateIsKeptRaw() {
        stub.respond(Reply.json(429, Contract.problem(429, "concurrency_limit", "wait_then_retry", "not_charged", 0).toString())
                .header("Retry-After", "Wed, 30 Sep 2026 12:00:00 GMT"));

        GenderApiHttpException error = assertThrows(GenderApiHttpException.class, () -> client.name("Onur"));

        assertEquals("Wed, 30 Sep 2026 12:00:00 GMT", error.getRetryAfter());
        assertNull(error.getRetryAfterSeconds());
    }

    @Test
    void providerFailureIsNotRetried() {
        stub.respond(Reply.json(502, Contract.problem(502, "ai_upstream_error", "inspect_billing_before_retry", "confirmed", 0).toString()));

        GenderApiHttpException error = assertThrows(GenderApiHttpException.class,
                () -> client.gender(GenderRequest.name("Onur").aiMode(AiMode.ALWAYS)));

        assertEquals(502, error.getStatus());
        assertEquals("ai_upstream_error", error.getCode());
        assertEquals("confirmed", error.getBillingStatus());
        assertFalse(error.isBillingUnconfirmed());
        assertTrue(error.getBatchItems().isEmpty());
        assertEquals(1, stub.requests().size());
    }

    @Test
    void unconfirmedBillingIsSurfacedWithoutRetry() {
        respondExample("/api/v2/gender", "503", "unconfirmed");

        GenderApiHttpException error = assertThrows(GenderApiHttpException.class, () -> client.name("Onur"));

        assertEquals(503, error.getStatus());
        assertEquals("billing_reconciliation_required", error.getCode());
        assertEquals("contact_support", error.getAction());
        assertEquals("unconfirmed", error.getBillingStatus());
        assertTrue(error.isBillingUnconfirmed());
        assertNull(error.getChargedCredits());
        assertNull(error.getRemainingCredits());
        assertEquals("11111111-1111-4111-8111-111111111111", error.getRequestId());
        assertEquals(1, stub.requests().size());
    }

    @Test
    void nonJsonProxyErrorKeepsStatusHeaderIdAndRawBody() {
        stub.respond(Reply.json(502, "<html><body>502 Bad Gateway</body></html>")
                .contentType("text/html").header("X-Request-ID", "proxy-123").header("Retry-After", "5"));

        GenderApiHttpException error = assertThrows(GenderApiHttpException.class, () -> client.name("Onur"));

        assertEquals(502, error.getStatus());
        assertNull(error.getCode());
        assertNull(error.getProblem());
        assertNull(error.getBillingStatus());
        assertEquals("proxy-123", error.getRequestId());
        assertEquals("5", error.getRetryAfter());
        assertTrue(error.getRawBody().contains("Bad Gateway"));
        assertEquals(1, stub.requests().size());
    }

    @Test
    void requestIdFallsBackFromMetaToBodyToHeader() {
        ObjectNode body = Contract.problem(500, "internal_error", "contact_support", "not_charged", 0);
        body.remove("meta");
        body.put("request_id", "body-id");
        stub.respond(Reply.json(500, body.toString()).header("X-Request-ID", "hdr-id"));
        assertEquals("body-id", assertThrows(GenderApiHttpException.class, () -> client.name("Onur")).getRequestId());

        body.remove("request_id");
        stub.respond(Reply.json(500, body.toString()).header("X-Request-ID", "hdr-id"));
        assertEquals("hdr-id", assertThrows(GenderApiHttpException.class, () -> client.name("Onur")).getRequestId());
    }

    // ------------------------------------------------------------------ transport safety

    @Test
    void redirectsAreRejectedAndNotFollowed() {
        for (int status : new int[] {301, 302, 307, 308}) {
            stub.respond(r -> r.path.endsWith("/elsewhere")
                    ? Reply.json(200, Contract.json(Contract.example("/api/v2/gender", "post", "200", "dataset")))
                    : Reply.json(status, "").contentType(null).header("Location", stub.origin() + "/elsewhere"));
            int before = stub.requests().size();

            GenderApiTransportException error = assertThrows(GenderApiTransportException.class, () -> client.name("Onur"));

            assertEquals(GenderApiTransportException.Kind.REDIRECT, error.getKind());
            assertEquals(status, error.getStatus());
            assertEquals(before + 1, stub.requests().size(), "redirect target must not be requested");
            assertTrue(stub.requests().stream().noneMatch(r -> r.path.endsWith("/elsewhere")));
        }
    }

    @Test
    void timeoutRaisesTransportErrorAndDoesNotRetry() {
        stub.respond(Reply.json(200, Contract.json(Contract.example("/api/v2/gender", "post", "200", "dataset"))).delay(3000));
        GenderApiClient fast = clientBuilder().apiKey(KEY).timeout(Duration.ofMillis(300)).build();

        long start = System.nanoTime();
        GenderApiTransportException error = assertThrows(GenderApiTransportException.class, () -> fast.name("Onur"));
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertTrue(error.isTimeout());
        assertTrue(elapsedMillis < 2500, "timeout should fire well before the stub answers: " + elapsedMillis);
        assertEquals(1, stub.requests().size());
    }

    @Test
    void defaultTimeoutIsTenSeconds() {
        assertEquals(Duration.ofSeconds(10), GenderApiClient.builder().build().getTimeout());
        assertThrows(GenderApiValidationException.class, () -> GenderApiClient.builder().timeout(Duration.ZERO));
        assertThrows(GenderApiValidationException.class, () -> GenderApiClient.builder().timeout(Duration.ofSeconds(-1)));
    }

    @Test
    void connectionFailureIsATransportError() {
        String deadBase = stub.baseUrl();
        stub.close();
        GenderApiClient offline = GenderApiClient.builder().baseUrl(deadBase).environment(n -> null)
                .timeout(Duration.ofSeconds(2)).build();

        GenderApiTransportException error = assertThrows(GenderApiTransportException.class, () -> offline.name("Onur"));
        assertTrue(error.getKind() == GenderApiTransportException.Kind.CONNECTION || error.isTimeout());
    }

    @Test
    void successWithoutJsonObjectIsInvalidResponse() {
        stub.respond(Reply.json(200, "<html>ok</html>").contentType("text/html").header("X-Request-ID", "r1"));
        GenderApiTransportException html = assertThrows(GenderApiTransportException.class, () -> client.name("Onur"));
        assertEquals(GenderApiTransportException.Kind.INVALID_RESPONSE, html.getKind());
        assertEquals("r1", html.getRequestId());

        stub.respond(Reply.json(200, "[1,2]"));
        assertEquals(GenderApiTransportException.Kind.INVALID_RESPONSE,
                assertThrows(GenderApiTransportException.class, () -> client.name("Onur")).getKind());

        stub.respond(Reply.json(200, "{broken"));
        assertEquals(GenderApiTransportException.Kind.INVALID_RESPONSE,
                assertThrows(GenderApiTransportException.class, () -> client.name("Onur")).getKind());
        assertEquals(3, stub.requests().size());
    }

    @Test
    void constructingTheClientMakesNoRequest() {
        GenderApiClient.builder().baseUrl(stub.baseUrl()).apiKey(KEY).build();
        new GenderApiClient(KEY);
        new GenderApiClient();
        assertTrue(stub.requests().isEmpty());
    }

    @Test
    void baseUrlMustBeHttpsExceptLocalhost() {
        assertEquals(GenderApiClient.DEFAULT_BASE_URL, GenderApiClient.builder().build().getBaseUrl());
        assertEquals("https://staging.example.com/api/v2",
                GenderApiClient.builder().baseUrl("https://staging.example.com/api/v2/").build().getBaseUrl());
        GenderApiClient.builder().baseUrl("http://localhost:8080/api/v2").build();
        GenderApiClient.builder().baseUrl("http://127.0.0.1:8080/api/v2").build();
        GenderApiClient.builder().baseUrl("http://[::1]:8080/api/v2").build();

        for (String bad : Arrays.asList("http://api.genderapi.io/api/v2", "ftp://127.0.0.1/", "api.genderapi.io",
                "https://user:pw@api.genderapi.io/api/v2", "https://api.genderapi.io/api/v2?key=x", "", "http://localhost.evil.com/")) {
            assertThrows(GenderApiValidationException.class, () -> GenderApiClient.builder().baseUrl(bad).build(), bad);
        }
    }

    // ------------------------------------------------------------------ client-side validation

    @Test
    void invalidInputFailsWithoutNetwork() {
        String longValue = String.join("", Collections.nCopies(255, "a"));
        String longId = String.join("", Collections.nCopies(65, "i"));
        List<Executable> cases = new ArrayList<>(Arrays.asList(
                () -> client.gender((GenderRequest) null),
                () -> client.gender(null, "Onur"),
                () -> client.name(null),
                () -> client.name(""),
                () -> client.name("   "),
                () -> client.name(longValue),
                () -> client.name("On\u0000ur"),
                () -> client.name("On\nur"),
                () -> client.name("On\u007fur"),
                () -> client.name("Onur", "tr"),
                () -> client.name("Onur", "TUR"),
                () -> client.name("Onur", ""),
                () -> client.gender(GenderRequest.name("Onur").id("")),
                () -> client.gender(GenderRequest.name("Onur").id(longId)),
                () -> client.gender(GenderRequest.name("Onur").aiMode(AiMode.OFF).forceToGenderize(true)),
                () -> client.gender(GenderRequest.name("Onur").aiMode(AiMode.ALWAYS).forceToGenderize(true)),
                () -> client.genderBatch(Collections.<GenderRequest>emptyList()),
                () -> client.genderBatch((List<GenderRequest>) null),
                () -> client.genderBatch(Collections.nCopies(51, GenderRequest.name("Onur"))),
                () -> client.genderBatch(GenderRequest.name("A").id("x"), GenderRequest.name("B").id("x")),
                () -> client.genderBatch(GenderRequest.name("A"), null),
                () -> client.genderBatch(GenderRequest.name("A"), GenderRequest.email("")),
                () -> client.validatePhone(null),
                () -> client.validatePhone("12"),
                () -> client.validatePhone("+90 abc 123"),
                () -> client.validatePhone("+90 555 000 00 00", "tr")));
        for (int i = 0; i < cases.size(); i++) {
            assertThrows(GenderApiValidationException.class, cases.get(i), "case " + i);
        }
        assertTrue(stub.requests().isEmpty(), "validation errors must not reach the network");
    }

    @Test
    void validationNamesTheOffendingBatchField() {
        GenderApiValidationException error = assertThrows(GenderApiValidationException.class,
                () -> client.genderBatch(GenderRequest.name("A"), GenderRequest.name("B").country("usa")));
        assertEquals("items[1].country", error.getField());
        assertTrue(error instanceof GenderApiException);
    }

    @Test
    void boundaryValuesAreAccepted() {
        respondExample("/api/v2/gender", "200", "dataset");
        client.name(String.join("", Collections.nCopies(254, "a")));
        client.gender(GenderRequest.name("Onur").id(String.join("", Collections.nCopies(64, "i"))));
        client.gender(GenderRequest.name("Onur").aiMode(AiMode.FALLBACK).forceToGenderize(true));
        stub.respond(Reply.json(200, Contract.json(Contract.example("/api/v2/gender/batch", "post", "200", "batch"))));
        client.genderBatch(Collections.nCopies(50, GenderRequest.name("Onur")));
        assertEquals(4, stub.requests().size());
    }

    // ------------------------------------------------------------------ other endpoints

    @Test
    void usageIsAnAuthenticatedGet() {
        stub.respond(Reply.json(200, Contract.json(Contract.example("/api/v2/usage", "get", "200", "usage"))));

        UsageResponse usage = client.usage();

        assertEquals(9L, usage.getData().getRemainingCredits());
        assertEquals("2026-09-26T12:00:00.000Z", usage.getData().getExpiresAt());
        assertEquals(10, usage.getData().getLimit());
        assertEquals("not_charged", usage.getMeta().getUsage().getBillingStatus());
        StubServer.Recorded request = stub.only();
        assertEquals("GET", request.method);
        assertEquals("/api/v2/usage", request.path);
        assertEquals("Bearer " + KEY, request.header("Authorization"));
        assertEquals("", request.body);
    }

    @Test
    void validatePhoneSendsNumberAndCountry() throws Exception {
        stub.respond(Reply.json(200, Contract.json(Contract.example("/api/v2/phone/validate", "post", "200", "phone"))));

        PhoneResponse response = client.validatePhone("+90 (555) 000-0000", "TR");

        assertEquals(Boolean.FALSE, response.getData().getValid());
        assertEquals(Boolean.FALSE, response.getData().getPossible());
        assertNull(response.getData().getE164());
        assertNull(response.getData().getCountryCallingCode());
        assertEquals(1, response.getMeta().getUsage().getChargedCredits());
        assertEquals("/api/v2/phone/validate", stub.only().path);
        assertEquals(Contract.MAPPER.readTree("{\"number\":\"+90 (555) 000-0000\",\"country\":\"TR\"}"), sentBody());
    }

    @Test
    void capabilitiesAndErrorCatalogNeedNoCredentials() {
        stub.respond(r -> r.path.equals("/api/v2/errors")
                ? Reply.json(200, Contract.errorCatalogBody())
                : Reply.json(200, "{\"version\":\"2\",\"limits\":{\"batch_items\":50}}"));

        JsonNode capabilities = client.capabilities();
        ErrorCatalog catalog = client.errorCatalog();

        assertEquals(50, capabilities.path("limits").path("batch_items").asInt());
        assertEquals(Arrays.asList(429), catalog.getEntry("rate_limit_exceeded").getHttpStatuses());
        assertEquals("wait_then_retry", catalog.getEntry("rate_limit_exceeded").getAction());
        assertEquals("contact_support", catalog.getEntry("billing_reconciliation_required").getAction());
        assertTrue(catalog.getErrors().size() >= 30);
        assertEquals("/api/v2", stub.requests().get(0).path);
        assertEquals("/api/v2/errors", stub.requests().get(1).path);
        assertTrue(stub.requests().stream().allMatch(r -> r.header("Authorization") == null && r.method.equals("GET")));
    }
}
