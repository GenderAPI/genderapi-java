package io.genderapi;

import io.genderapi.model.GenderApiResult;
import io.genderapi.model.GenderSuccessResponse;
import io.genderapi.model.GenderErrorResponse;
import io.genderapi.exception.GenderApiException;
import org.junit.Assert;
import org.junit.Test;

public class GenderApiClientTest {

    /**
     * Test with a valid API key and a common name.
     * Make sure to replace YOUR_API_KEY with your real key
     * if you want to try a live test.
     */
    @Test
    public void testGetGenderByName() {
        // TODO: replace with your real API key for a live test
        String apiKey = "YOUR_API_KEY";

        GenderApiClient client = new GenderApiClient(apiKey);

        try {
            GenderApiResult result = client.getGenderByName(
                    "Michael",
                    "US",
                    false,
                    false
            );

            Assert.assertNotNull(result);

            if (result instanceof GenderSuccessResponse) {
                GenderSuccessResponse success = (GenderSuccessResponse) result;
                System.out.println("Gender: " + success.getGender());
                Assert.assertTrue(success.isStatus());
                Assert.assertNotNull(success.getGender());
            } else if (result instanceof GenderErrorResponse) {
                GenderErrorResponse error = (GenderErrorResponse) result;
                System.out.println("API returned error: " + error.getErrmsg());
                Assert.fail("Expected a successful result, but got error: " + error.getErrmsg());
            } else {
                Assert.fail("Unexpected result type.");
            }

        } catch (GenderApiException e) {
            Assert.fail("API call failed: " + e.getMessage());
        }
    }



}
