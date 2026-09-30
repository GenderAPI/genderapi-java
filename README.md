# genderapi-java

Official GenderAPI.io V2 client for Java (`io.genderapi:genderapi` 2.0.0).

It infers a likely gender from a **name**, **email address** or **username**, and validates phone numbers, through the [GenderAPI.io V2 API](https://www.genderapi.io/api-documentation). Results are inferences and can be `unknown`. They do not verify a person's identity, and the country fields do not establish nationality, residence or ethnicity.

> **1.x (V1 API) stays available and installable indefinitely; no deprecation or shutdown is planned.** To keep using it, pin 1.x from JitPack: `com.github.GenderAPI:genderapi-java:v1.0.3` (see [Staying on 1.x](#staying-on-1x)). The source stays on the [`v1` branch](https://github.com/GenderAPI/genderapi-java/tree/v1). 2.0.0 is a breaking rewrite for the V2 API with new coordinates, `io.genderapi:genderapi`. See [Migrating from 1.x](#migrating-from-1x).

- Java 11+ (`java.net.http.HttpClient`), one runtime dependency (Jackson databind)
- No automatic retries, no redirects, 10-second default timeout
- Server-side use only

## Installation

The artifact is published to **GitHub Packages**. It is not on Maven Central.

GitHub Packages requires authentication, even for public packages. Create a GitHub personal access token (classic) with the `read:packages` scope and add it to `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github-genderapi</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_TOKEN_WITH_READ_PACKAGES</password>
    </server>
  </servers>
</settings>
```

### Maven

```xml
<repositories>
  <repository>
    <id>github-genderapi</id>
    <url>https://maven.pkg.github.com/GenderAPI/genderapi-java</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>io.genderapi</groupId>
    <artifactId>genderapi</artifactId>
    <version>2.0.0</version>
  </dependency>
</dependencies>
```

### Gradle

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/GenderAPI/genderapi-java")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN") // read:packages
        }
    }
}

dependencies {
    implementation "io.genderapi:genderapi:2.0.0"
}
```

### Alternative: JitPack

JitPack builds tagged versions from this repository without a GitHub token. The coordinates differ:

```xml
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>

<dependency>
  <groupId>com.github.GenderAPI</groupId>
  <artifactId>genderapi-java</artifactId>
  <version>v2.0.0</version>
</dependency>
```

## Quick start

Set your API key in the server environment:

```bash
export GENDERAPI_API_KEY=your_api_key
```

```java
import io.genderapi.GenderApiClient;
import io.genderapi.model.GenderResponse;
import io.genderapi.model.Prediction;

GenderApiClient client = new GenderApiClient(); // reads GENDERAPI_API_KEY; makes no request

GenderResponse response = client.name("Andrea", "IT");
Prediction data = response.getData();

if (data.isIdentified()) {
    System.out.println(data.getGender() + " " + data.getConfidence() + " " + data.getConfidenceKind());
} else {
    System.out.println("unknown: " + data.getReason()); // still a successful, billable result
}
System.out.println(response.getMeta().getUsage().getChargedCredits());
```

You can also pass the key explicitly with `new GenderApiClient(apiKey)` or `GenderApiClient.builder().apiKey(apiKey).build()`.

### Email, username and options

```java
import io.genderapi.AiMode;
import io.genderapi.GenderRequest;

client.email("alex.smith@example.com");
client.username("alex_dev", "US");

client.gender(GenderRequest.name("Andrea")
        .country("IT")            // optional ISO 3166-1 alpha-2, uppercase
        .aiMode(AiMode.OFF)       // off | fallback | always
        .id("row-17"));           // optional reference, 1-64 characters

client.gender(GenderRequest.username("prenses")
        .forceToGenderize(true)); // dataset first, then nickname-aware AI
```

### Batch

```java
import io.genderapi.model.BatchItem;
import io.genderapi.model.BatchResponse;

BatchResponse batch = client.genderBatch(
        GenderRequest.name("Andrea").country("IT").id("a"),
        GenderRequest.email("alex@example.com").id("b"),
        GenderRequest.username("prenses").id("c"));

for (BatchItem item : batch.getItems()) {
    if (item.isSuccess()) {
        System.out.println(item.getId() + " " + item.getData().getGender());
    } else {
        System.out.println(item.getId() + " failed: " + item.getError().getCode());
    }
}
System.out.println(batch.getSummary().getFailed() + " failed of " + batch.getSummary().getTotal());
```

A batch takes 1-50 items (10 on the IP trial; the server decides). Split larger jobs yourself. Partial success returns normally; check `hasFailures()` / `getFailedItems()`.

### Usage (free)

```java
UsageResponse usage = client.usage();
usage.getData().getRemainingCredits(); // can be negative
usage.getData().getExpiresAt();
```

### Phone validation and discovery

```java
PhoneResponse phone = client.validatePhone("+90 555 000 00 00", "TR");
phone.getData().getValid();
phone.getData().getE164();

JsonNode capabilities = client.capabilities(); // GET /api/v2, no credentials sent
ErrorCatalog catalog = client.errorCatalog();   // GET /api/v2/errors, no credentials sent
catalog.getEntry("rate_limit_exceeded").getAction(); // "wait_then_retry"
```

## Methods

| Method | HTTP | Notes |
| --- | --- | --- |
| `gender(GenderRequest)` | `POST /api/v2/gender` | Also `gender(type, value)` and `gender(type, value, country, aiMode, forceToGenderize, id)` |
| `name(value[, country])`, `email(...)`, `username(...)` | `POST /api/v2/gender` | Convenience forms |
| `genderBatch(List<GenderRequest>)` / `genderBatch(GenderRequest...)` | `POST /api/v2/gender/batch` | 1-50 items, unique ids |
| `usage()` | `GET /api/v2/usage` | Free |
| `validatePhone(number[, country])` | `POST /api/v2/phone/validate` | Billable |
| `capabilities()` | `GET /api/v2` | No auth |
| `errorCatalog()` | `GET /api/v2/errors` | No auth |

## Options

| Builder method | Default | Description |
| --- | --- | --- |
| `apiKey(String)` | `GENDERAPI_API_KEY` | Sent only as `Authorization: Bearer ...`. `null`/blank falls back to the environment variable. |
| `withoutApiKey()` | | Send no key and ignore the environment variable (IP trial). |
| `timeout(Duration)` | 10 seconds | Bounds the whole call (connect, send, read). |
| `baseUrl(String)` | `https://api.genderapi.io/api/v2` | For tests. Must be `https`; plain `http` is accepted only for `localhost`, `127.0.0.1` and `[::1]`. |
| `requireApiKeyAccess(boolean)` | `true` | Only effective when a key is configured. A successful response whose `meta.access.mode` is not `api_key` (usually `ip_trial` because the key was not recognized) throws `GenderApiAccessModeException`. Pass `false` to return such responses normally. Never applies to `capabilities()` or `errorCatalog()`. |

Request fields (wire names are sent exactly as the API defines them):

| `GenderRequest` | Wire field | Rule checked before sending |
| --- | --- | --- |
| `of(type, value)` / `name()` / `email()` / `username()` | `type`, `value` | type is name/email/username; value has 1-254 characters, is not blank, has no control characters |
| `.country("US")` | `country` | two uppercase letters (the API validates membership) |
| `.aiMode(AiMode.OFF)` | `options.ai_mode` | `off`, `fallback`, `always`. Server default: `fallback` for single requests, `off` for batch items |
| `.forceToGenderize(true)` | `forceToGenderize` | cannot be combined with `off` or `always` |
| `.id("row-1")` | `id` | 1-64 characters; unique within a batch |

Invalid input throws `GenderApiValidationException` **without a network call**. The API performs the authoritative validation (for example email syntax and country membership) and answers with 422 otherwise.

Credit rules are product rules; `meta.usage` in each response is authoritative. Single requests default to AI fallback (1 credit total). `ai_mode always` costs 2. `forceToGenderize` costs 1 for a dataset result and 2 total when AI is used. Successful unknown results are billable. A positive starting balance is enough, so a 2-credit request can leave `-1`.

## Response fields

Every response object keeps the full JSON: `raw()` returns it and `get("field")` reads any field, including fields added in later API versions. Typed getters return `null` for JSON `null` and never rescale values.

`GenderResponse.getData()` (`Prediction`):

| Getter | Field | Notes |
| --- | --- | --- |
| `getGender()` | `gender` | `male`, `female` or `null` |
| `getResultStatus()`, `isIdentified()`, `isUnknown()` | `result_status` | `identified` or `unknown` (an inference status, not identity verification) |
| `getReason()` | `reason` | `null`, `not_found`, `no_name_candidate`, `ambiguous`, `insufficient_evidence` |
| `getConfidence()` | `confidence` | 0-1 as returned, `null` when unknown. Not a percentage and not a measured accuracy |
| `getConfidenceKind()` | `confidence_kind` | `observed_frequency` (dataset share) or `model_reported` (model score, not a calibrated probability) |
| `getSampleCount()` | `sample_count` | dataset samples; `null` for AI |
| `getSource()` | `source` | `dataset`, `ai`, `none` |
| `getName()`, `getCountry()`, `getCountrySource()` | `name`, `country`, `country_source` | |
| `getMatch()` | `match` | `name`, `method`, `scope`, `country` |
| `getInput()` | `input` | echoed `type`, `value`, `country`, `forceToGenderize` |

`getMeta()` (`Meta`): `getRequestId()`, `getDurationMs()`, `getAccess()` (`mode`: `api_key`, `ip_trial`, `unauthenticated`; `reason`), `getUsage()` and, for batches, `getSummary()` (`total`, `succeeded`, `identified`, `unknown`, `failed`).

`Usage`: `getBillingStatus()` (`not_charged`, `confirmed`, `unconfirmed`), `getChargedCredits()` (`null` when unconfirmed), `getRemainingCredits()` (can be negative or `null`), `getResetsAt()`, `getLimit()`, `getPeriodSeconds()`.

`BatchItem`: `getIndex()`, `getId()`, `getChargedCredits()`, and exactly one of `getData()` / `getError()`.

## Errors

All exceptions are unchecked and extend `GenderApiException`. Messages never include the API key or submitted values.

| Exception | When | Useful getters |
| --- | --- | --- |
| `GenderApiValidationException` | Invalid input or configuration, before any request | `getField()` |
| `GenderApiHttpException` | HTTP 400 or above | `getStatus()`, `getCode()`, `getTitle()`, `getDetail()`, `getAction()`, `getErrors()` (validation pointers), `getRequestId()`, `getRetryAfter()`, `getRetryAfterSeconds()`, `getBillingStatus()`, `getChargedCredits()`, `getRemainingCredits()`, `getBatchItems()`, `getRawBody()` |
| `GenderApiTransportException` | Timeout, connection failure, 3xx redirect, or a 2xx body that is not a JSON object | `getKind()`, `isTimeout()`, `getStatus()`, `getRequestId()` |
| `GenderApiAccessModeException` | A key is configured but a successful response reports another access mode (e.g. `ip_trial`); code `unexpected_access_mode` | `getAccessMode()`, `getAccessReason()`, `getResult()` / `getResult(GenderResponse.class)`, `getJson()`, `getUsage()`, `getStatus()`, `getRequestId()` |

```java
try {
    client.name("Andrea");
} catch (GenderApiHttpException e) {
    switch (e.getCode() == null ? "" : e.getCode()) {
        case "rate_limit_exceeded":
        case "concurrency_limit":
            // wait e.getRetryAfter() before sending a new request
            break;
        case "insufficient_credits":
            // add credits or wait for e.getUsage().getResetsAt()
            break;
        default:
            if (e.isBillingUnconfirmed()) {
                // contact support with e.getRequestId(); do not retry automatically
            }
    }
} catch (GenderApiTransportException e) {
    // no usable response: the request may still have completed and been billed
} catch (GenderApiAccessModeException e) {
    // the key was not used (e.getAccessMode() is e.g. "ip_trial"); the request was already processed
    GenderResponse result = e.getResult(GenderResponse.class);
}
```

`GenderApiAccessModeException` is thrown after the request has already been processed: it may have consumed IP-trial credits, and it is not retried. The complete response the method would have returned is on the exception (`getResult()`, typed with `getResult(Class)`, or as JSON with `getJson()`). Check the key; disable the check with `requireApiKeyAccess(false)`.

Match on `getCode()`, never on the human-readable detail. `getRequestId()` comes from `meta.request_id`, then the body's `request_id`, then the `X-Request-ID` header. Proxy failures can return non-JSON bodies; then `getCode()` is `null` and `getRawBody()` holds the response. When every executed batch item fails, the API returns an error status and `getBatchItems()` holds the rows. The public catalog of codes and actions is at `https://api.genderapi.io/api/v2/errors` (`client.errorCatalog()`).

## Billing and retries

- The SDK **never retries** a request, including on 429. Every request is a new operation with normal billing, and a lost response does not mean the request was unbilled.
- **429**: wait for `Retry-After`, then send a new request if you still need it.
- **`billing_status: unconfirmed`**: contact support with the request ID before retrying.
- **Prediction failures (502/503/504)**: inspect `getBillingStatus()` and fix the cause before retrying.
- **Partial batch success**: retry only the failed items, and only once billing is confirmed. Resubmitting succeeded items charges them again.
- **Timeouts and connection errors** (`GenderApiTransportException`): check `client.usage()` before sending again.
- Redirects are never followed, so the key is never forwarded to another host.

## IP trial (no key)

Without an API key the client still works: the server applies the shared IP trial (10 credits per 24 hours; batches of up to 10 items). The SDK implements no trial logic of its own. A missing or unknown key can silently use trial credits, so check `response.getMeta().getAccess().getMode()`: it is `api_key` when your key was used and `ip_trial` otherwise (`getReason()` says why). When a key is configured, the client does this check for you by default and throws `GenderApiAccessModeException` (with the full result attached) instead of returning a trial response; see `requireApiKeyAccess(boolean)`. Disabled, expired or restricted keys are rejected and do not fall back to the trial.

## Keep your key on the server

Use this library only in server-side code (backends, jobs, CLIs). Do not ship your API key in Android apps, desktop apps, applets or any code that runs on user devices; anyone can extract it. Read the key from the environment or a secret manager. The SDK never logs the key and never puts it in a URL. Response bodies (`raw()`, `toString()`, `getRawBody()`) contain the submitted input, which can be personal data: do not log them wholesale.

The client is immutable and thread-safe; create one instance and reuse it.

## Migrating from 1.x

| 1.x (V1) | 2.0.0 (V2) |
| --- | --- |
| `getGenderByName(name, country, askToAI, forceToGenderize)` | `name(name, country)` or `gender(GenderRequest.name(name).country(c).aiMode(...).forceToGenderize(...))` |
| `getGenderByEmail(...)`, `getGenderByUsername(...)` | `email(...)`, `username(...)` |
| V1 routes `/api`, `/api/email`, `/api/username` | `POST /api/v2/gender` with `type` and `value` |
| V1 multi routes (`/api/name/multi/country`, ...) | `POST /api/v2/gender/batch` with `items` (types may be mixed) via `genderBatch(...)` |
| `/api/phone` | `POST /api/v2/phone/validate` via `validatePhone(number, country)` |
| `askToAI` | `options.ai_mode` (`AiMode.FALLBACK`, `ALWAYS`, `OFF`); single requests already default to `fallback` |
| `forceToGenderize` | Same field for name, email and username; cannot combine with `off` or `always` |
| Flat response fields | `data` (result) and `meta` (request, access, billing) |
| `probability` (percentage) | `confidence` (0-1) plus `confidence_kind`; AI scores are not calibrated probabilities |
| `total_names` | `data.sample_count` (nullable) |
| `used_credits` | `meta.usage.charged_credits` (plus `billing_status`) |
| `remaining_credits` | `meta.usage.remaining_credits` (can be negative or null) |
| `status` / `errno` / `errmsg` | HTTP status plus Problem Details `code` and `action` (`GenderApiHttpException`) |
| `GenderErrorResponse`, checked `GenderApiException` | Unchecked `GenderApiHttpException`, `GenderApiTransportException`, `GenderApiValidationException` |
| OkHttp + Jackson, Java 8 | `java.net.http` + Jackson, Java 11+ |
| JitPack `com.github.GenderAPI:genderapi-java` | GitHub Packages `io.genderapi:genderapi` (JitPack still possible) |

Your API key and credit balance stay the same. You can migrate at your own pace.

### Staying on 1.x

1.x (V1 API) stays available and installable indefinitely; no deprecation or shutdown is planned. To keep using it, pin 1.x from JitPack. The source stays on the [`v1` branch](https://github.com/GenderAPI/genderapi-java/tree/v1). Note that 2.0 uses different coordinates (`io.genderapi:genderapi`), so the two do not replace each other by accident.

Maven:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.GenderAPI</groupId>
  <artifactId>genderapi-java</artifactId>
  <version>v1.0.3</version>
</dependency>
```

Gradle:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.GenderAPI:genderapi-java:v1.0.3'
}
```

## Documentation

- [API documentation](https://www.genderapi.io/api-documentation)
- [Authentication and free trial](https://www.genderapi.io/docs/v2/authentication)
- [Request parameters](https://www.genderapi.io/docs/v2/request-parameters)
- [Gender from name](https://www.genderapi.io/docs/v2/gender-from-name), [email](https://www.genderapi.io/docs/v2/gender-from-email), [username](https://www.genderapi.io/docs/v2/gender-from-username)
- [Batch](https://www.genderapi.io/docs/v2/batch)
- [Phone validation](https://www.genderapi.io/docs/v2/phone-validation)
- [Responses](https://www.genderapi.io/docs/v2/responses)
- [AI options](https://www.genderapi.io/docs/v2/ai-options)
- [Credits and usage](https://www.genderapi.io/docs/v2/credits-and-usage)
- [Errors and retries](https://www.genderapi.io/docs/v2/errors-and-retries)
- [Migrating from V1](https://www.genderapi.io/docs/v2/migration)
- OpenAPI: `https://api.genderapi.io/api/v2/openapi.json`

## Development

```bash
mvn verify   # JDK 11+; tests use a local stub server, never the real API
```

The tests load their fixtures from the examples in `src/test/resources/contract/openapi-v2.json`, so the SDK is checked against the published contract.

## Publishing (maintainers)

**GitHub Packages (default).** Push a tag that matches the POM version, for example `v2.0.0`. `.github/workflows/maven-publish.yml` checks the tag against `pom.xml`, runs the tests and deploys with the workflow's `GITHUB_TOKEN` (`packages: write`). No other secret is needed.

**Maven Central (not set up).** Publishing there would need:

1. A Sonatype Central Portal account at https://central.sonatype.com.
2. Namespace verification for `io.genderapi`: add the TXT record the portal generates to the `genderapi.io` DNS zone.
3. A GPG signing key, with its public key uploaded to a public key server (for example `keys.openpgp.org`).
4. A release profile in `pom.xml` adding `maven-gpg-plugin` (sign) and `org.sonatype.central:central-publishing-maven-plugin` (upload). The POM already has the required name, description, URL, license, developer and SCM data and builds sources and javadoc jars.
5. Repository secrets for a workflow, for example `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_TOKEN` (a portal user token), `MAVEN_GPG_PRIVATE_KEY` and `MAVEN_GPG_PASSPHRASE`, used through `actions/setup-java` (`server-id: central`, `gpg-private-key`, `gpg-passphrase`).

Central releases are immutable. Publish the same version to both channels only from the same tag.

## License

MIT License. See [LICENSE](LICENSE).
