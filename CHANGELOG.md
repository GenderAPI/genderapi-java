# Changelog

All notable changes to `io.genderapi:genderapi` are documented here.

## 2.0.0 - 2026-09-30

### Breaking

- Rewritten as a client for the GenderAPI.io **V2** API (`https://api.genderapi.io/api/v2`). The V1 methods (`getGenderByName`, `getGenderByEmail`, `getGenderByUsername`), models (`GenderSuccessResponse`, `GenderErrorResponse`, `GenderApiResult`) and V1 routes are removed. 1.x (V1 API) stays available and installable indefinitely, with no deprecation or shutdown planned: pin it from JitPack with `com.github.GenderAPI:genderapi-java:v1.0.3` (repository `https://jitpack.io`). The `v1` branch keeps the source.
- Requires Java 11+ (uses `java.net.http.HttpClient`). OkHttp is no longer a dependency; Jackson databind is the only runtime dependency.
- Exceptions are unchecked: `GenderApiValidationException`, `GenderApiHttpException`, `GenderApiTransportException`, `GenderApiAccessModeException` (all extend `GenderApiException`).
- `askToAI` is replaced by `options.ai_mode` (`AiMode.OFF`, `FALLBACK`, `ALWAYS`); `probability` by `confidence` (0-1) plus `confidence_kind`; `used_credits` by `meta.usage.charged_credits`; `status`/`errno` by HTTP status plus Problem Details `code` and `action`.
- Published to GitHub Packages as `io.genderapi:genderapi`; the publish workflow now runs on `v*` tags. The unused Gradle publish workflow was removed.

### Added

- `gender(...)`, `name(...)`, `email(...)`, `username(...)`, `genderBatch(...)`, `usage()`, `validatePhone(...)`, `capabilities()`, `errorCatalog()`.
- Typed accessors for `result_status`, `reason`, `match`, `country_source`, `confidence_kind`, `sample_count`, `source`, batch items, `meta.summary`, `meta.access` and `meta.usage` (`billing_status`, `charged_credits`, `remaining_credits`, `resets_at`). Every response keeps its full JSON (`raw()`), including unknown fields.
- Structured HTTP errors with `status`, `code`, `title`, `detail`, `action`, validation `errors`, `request_id`, `Retry-After`, billing status, raw body and all-failed batch rows.
- Client-side validation mirroring the schema (no network call on invalid input).
- API key from the constructor, the builder or `GENDERAPI_API_KEY`; without a key the server applies the shared IP trial.
- Access-mode check: when a key is configured, a successful response whose `meta.access.mode` is not `api_key` (e.g. `ip_trial`) throws `GenderApiAccessModeException` (code `unexpected_access_mode`) carrying the full result, access mode and reason, HTTP status and request ID. The request was already processed and is not retried. Opt out with `GenderApiClient.builder().requireApiKeyAccess(false)`. Not applied to `capabilities()` or `errorCatalog()`.
- CI workflow testing Java 11, 17 and 21.

### Safety

- No automatic retries (including 429), no redirects followed, 10-second default timeout for the whole call, HTTPS-only base URL (plain HTTP only for localhost in tests), key sent only as a Bearer header, no request at construction.
