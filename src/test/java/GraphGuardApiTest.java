
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GraphGuardApiTest {

    private static final String BASE_URL =
            "http://127.0.0.1:8080";

    public static void main(String[] args) throws Exception {

        HttpClient client = HttpClient.newHttpClient();

        // Test 1: Health endpoint
        HttpRequest healthRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/health"))
                .GET()
                .build();

        HttpResponse<String> health = client.send(
                healthRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        if (health.statusCode() != 200
                || !health.body().contains("\"service\":\"GraphGuard\"")) {
            throw new AssertionError("Health endpoint failed");
        }

        System.out.println("PASS: Health endpoint");

        // Test 2: Transactions endpoint
        HttpRequest transactionsRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/transactions"))
                .GET()
                .build();

        HttpResponse<String> transactions = client.send(
                transactionsRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        if (transactions.statusCode() != 200) {
            throw new AssertionError("Transactions endpoint failed");
        }

        int transactionCount =
                transactions.body().split("\"id\":", -1).length - 1;

        if (transactionCount != 10) {
            throw new AssertionError(
                    "Expected 10 transactions, got " + transactionCount
            );
        }

        System.out.println("PASS: Transactions endpoint");

        // Test 3: High-risk filter
        HttpRequest highRiskRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/transactions?risk=high"))
                .GET()
                .build();

        HttpResponse<String> highRisk = client.send(
                highRiskRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        if (highRisk.statusCode() != 200) {
            throw new AssertionError("High-risk endpoint failed");
        }

        int highRiskCount =
                highRisk.body().split("\"id\":", -1).length - 1;

        if (highRiskCount != 10
                || highRisk.body().contains("\"riskLevel\":\"LOW RISK\"")
                || highRisk.body().contains("\"riskLevel\":\"MEDIUM RISK\"")) {
            throw new AssertionError("High-risk filter failed");
        }

        System.out.println("PASS: High-risk filter");

        // Test 4: Method restriction
        HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/health"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> postResponse = client.send(
                postRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        if (postResponse.statusCode() != 405) {
            throw new AssertionError("Expected HTTP 405 for POST");
        }

        System.out.println("PASS: HTTP method restriction");

        System.out.println("\nAll GraphGuard API tests passed.");
    }
}
