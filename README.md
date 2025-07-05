# genderapi-java

Official Java SDK for [GenderAPI.io](https://www.genderapi.io) — determine gender from **names**, **emails**, and **usernames** using AI.

---

Get Free API Key: [https://app.genderapi.io](https://app.genderapi.io)

---

## 🚀 Installation

First, install the SDK in your Maven project. Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.genderapi</groupId>
    <artifactId>genderapi</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or if you’re building manually:

```bash
mvn clean install
```

Then import the SDK in your Java code.

---

## 📝 Usage

### 🔹 Get Gender by Name

```java
import io.genderapi.GenderApiClient;
import io.genderapi.model.GenderSuccessResponse;
import io.genderapi.model.GenderErrorResponse;

public class Example {
    public static void main(String[] args) {
        GenderApiClient client = new GenderApiClient("YOUR_API_KEY");

        try {
            GenderSuccessResponse response = client.getGenderByName(
                "Michael", "US", false, false
            );
            System.out.println(response.getGender());
        } catch (GenderErrorResponse error) {
            System.out.println("API Error: " + error.getErrmsg());
        } catch (Exception ex) {
            System.out.println("Unexpected error: " + ex.getMessage());
        }
    }
}
```

---

### 🔹 Get Gender by Email

```java
GenderApiClient client = new GenderApiClient("YOUR_API_KEY");

try {
    GenderSuccessResponse response = client.getGenderByEmail(
        "michael.smith@example.com", "US", false
    );
    System.out.println(response.getGender());
} catch (GenderErrorResponse error) {
    System.out.println("API Error: " + error.getErrmsg());
} catch (Exception ex) {
    System.out.println("Unexpected error: " + ex.getMessage());
}
```

---

### 🔹 Get Gender by Username

```java
GenderApiClient client = new GenderApiClient("YOUR_API_KEY");

try {
    GenderSuccessResponse response = client.getGenderByUsername(
        "michael_dev", "US", false, false
    );
    System.out.println(response.getGender());
} catch (GenderErrorResponse error) {
    System.out.println("API Error: " + error.getErrmsg());
} catch (Exception ex) {
    System.out.println("Unexpected error: " + ex.getMessage());
}
```

---

## 📥 API Parameters

---

### Name Lookup

| Parameter            | Type     | Required | Description |
|-----------------------|----------|----------|-------------|
| name                  | String   | Yes      | Name to query. |
| country               | String   | No       | Two-letter country code (e.g. "US"). Helps narrow down gender detection results by region. |
| askToAI               | Boolean  | No       | Default is `false`. If `true`, sends the query directly to AI for maximum accuracy, consuming 3 credits per request. |
| forceToGenderize      | Boolean  | No       | Default is `false`. When `true`, analyzes even nicknames, emojis, or unconventional strings like "spider man" instead of returning `null` for non-standard names. |

---

### Email Lookup

| Parameter            | Type     | Required | Description |
|----------------------|----------|----------|-------------|
| email                | String   | Yes      | Email address to query. |
| country              | String   | No       | Two-letter country code (e.g. "US"). Helps narrow down gender detection results by region. |
| askToAI              | Boolean  | No       | Default is `false`. If `true`, sends the query directly to AI for maximum accuracy, consuming 3 credits per request. |

---

### Username Lookup

| Parameter            | Type     | Required | Description |
|----------------------|----------|----------|-------------|
| username             | String   | Yes      | Username to query. |
| country              | String   | No       | Two-letter country code (e.g. "US"). Helps narrow down gender detection results by region. |
| askToAI              | Boolean  | No       | Default is `false`. If `true`, sends the query directly to AI for maximum accuracy, consuming 3 credits per request. |
| forceToGenderize     | Boolean  | No       | Default is `false`. When `true`, analyzes even nicknames, emojis, or unconventional strings like "spider man" instead of returning `null` for non-standard names. |

---

## ✅ API Response

Example JSON response for all endpoints:

```json
{
  "status": true,
  "used_credits": 1,
  "remaining_credits": 4999,
  "expires": 1743659200,
  "q": "michael.smith@example.com",
  "name": "Michael",
  "gender": "male",
  "country": "US",
  "total_names": 325,
  "probability": 98,
  "duration": "4ms"
}
```

---

### Response Fields

| Field             | Type               | Description                                         |
|-------------------|--------------------|-----------------------------------------------------|
| status            | Boolean            | `true` or `false`. Check errors if false.           |
| used_credits      | Integer            | Credits used for this request.                     |
| remaining_credits | Integer            | Remaining credits on your package.                 |
| expires           | Integer (timestamp)| Package expiration date (in seconds).              |
| q                 | String             | Your input query (name, email, or username).       |
| name              | String             | Found name.                                        |
| gender            | Enum[String]       | `"male"`, `"female"`, or `"null"`.                |
| country           | Enum[String]       | Most likely country (e.g. `"US"`, `"DE"`, etc.).  |
| total_names       | Integer            | Number of samples behind the prediction.          |
| probability       | Integer            | Likelihood percentage (50-100).                   |
| duration          | String             | Processing time (e.g. `"4ms"`).                   |

---

## ⚠️ Error Handling

When `status` is `false`, the SDK throws a `GenderErrorResponse` exception containing details like:

| Field   | Type    | Description |
|---------|---------|-------------|
| status  | Boolean | `false` |
| errno   | Integer | Numeric error code. |
| errmsg  | String  | Human-readable error message. |

Example error JSON:

```json
{
  "status": false,
  "errno": 94,
  "errmsg": "invalid or missing key"
}
```

Typical error codes include:

| errno | errmsg                        | Description                                                       |
|-------|-------------------------------|-------------------------------------------------------------------|
| 50    | access denied                 | Unauthorized IP Address or Referrer. Check your access privileges. |
| 90    | invalid country code          | Check supported country codes. [ISO 3166-1 alpha-2](https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2) |
| 91    | name not set \|\| email not set | Missing `name` or `email` parameter on your request.         |
| 92    | too many names \|\| too many emails | Limit is 100 for names, 50 for emails in one request.     |
| 93    | limit reached                 | Your API key credit has been exhausted.                          |
| 94    | invalid or missing key        | Your API key is invalid or missing.                              |
| 99    | API key has expired           | Please renew your API key.                                       |

---

## 🔗 Live Test Pages

You can try live gender detection directly on GenderAPI.io:

- **Determine gender from a name:**  
  [www.genderapi.io](https://www.genderapi.io)

- **Determine gender from an email address:**  
  [https://www.genderapi.io/determine-gender-from-email](https://www.genderapi.io/determine-gender-from-email)

- **Determine gender from a username:**  
  [https://www.genderapi.io/determine-gender-from-username](https://www.genderapi.io/determine-gender-from-username)

---

## 📚 Detailed API Documentation

For the complete API reference, visit:

[https://www.genderapi.io/api-documentation](https://www.genderapi.io/api-documentation)

---

## ⚖️ License

MIT License
