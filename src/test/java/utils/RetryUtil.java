package utils;

import io.restassured.response.Response;

import java.util.function.Supplier;

public class RetryUtil {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_WAIT = 500; // 1 sec

    public static Response executeWithRetry(Supplier<Response> request) {

        int attempt = 0;
        long waitTime = INITIAL_WAIT;
        Response response = null;
        while (attempt < MAX_RETRIES) {
            attempt++;
            response = request.get();
            int statusCode = response.getStatusCode();
            // Retry only for server errors
            if (statusCode < 500) {
                return response;
            }
            System.out.println("Retry attempt " + attempt + " due to status: " + statusCode);
            try {
                Thread.sleep(waitTime);
                waitTime *= 2; // exponential backoff
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return response;
    }
}