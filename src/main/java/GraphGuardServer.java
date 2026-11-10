import java.util.List;
import java.util.Locale;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class GraphGuardServer {

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 8080),
                0
        );

        server.createContext("/api/health", exchange -> {

            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set(
                        "Content-Type", "application/json"
                );

                byte[] error = "{\"error\":\"Method not allowed\"}"
                        .getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Allow", "GET");
                exchange.sendResponseHeaders(405, error.length);

                try (OutputStream output =
                             exchange.getResponseBody()) {
                    output.write(error);
                }
                return;
            }

            String json = "{\"status\":\"ok\",\"service\":\"GraphGuard\"}";

            byte[] response =
                    json.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type", "application/json; charset=UTF-8"
            );

            exchange.sendResponseHeaders(200, response.length);

            try (OutputStream output =
                         exchange.getResponseBody()) {
                output.write(response);
            }
        });

        
        server.createContext("/api/transactions", exchange -> {

            if (!"GET".equals(exchange.getRequestMethod())) {
                byte[] error = "{\"error\":\"Method not allowed\"}"
                        .getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Allow", "GET");
                exchange.getResponseHeaders().set(
                        "Content-Type", "application/json; charset=UTF-8"
                );

                exchange.sendResponseHeaders(405, error.length);

                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(error);
                }
                return;
            }

            try {
                TransactionDataLoader loader = new TransactionDataLoader();

                
            String query = exchange.getRequestURI().getRawQuery();

            boolean highRiskOnly = false;

            if (query != null) {
                for (String parameter : query.split("&")) {
                    if ("risk=high".equalsIgnoreCase(parameter)) {
                        highRiskOnly = true;
                    }
                }
            }

            List<Transaction> transactions =
                    loader.loadTransactions("data/transactions.csv", 10000);


                StringBuilder json = new StringBuilder("[");
                FraudDetectionService fraudService =
                        new FraudDetectionService();

                

            int returnedCount = 0;

            for (Transaction t : transactions) {

                int riskScore = fraudService.calculateRiskScore(t);
                String riskLevel = fraudService.evaluateTransaction(t);

                if (highRiskOnly && !"HIGH RISK".equals(riskLevel)) {
                    continue;
                }

                if (returnedCount >= 10) {
                    break;
                }

                if (returnedCount > 0) {
                    json.append(",");
                }

                json.append(String.format(
                        Locale.ROOT,
                        "{\"id\":\"%s\",\"sender\":\"%s\","
                        + "\"receiver\":\"%s\",\"amount\":%.2f,"
                        + "\"type\":\"%s\",\"step\":%d,"
                        + "\"riskScore\":%d,\"riskLevel\":\"%s\"}",
                        t.getTransactionId(),
                        t.getSender(),
                        t.getReceiver(),
                        t.getAmount(),
                        t.getType(),
                        t.getStep(),
                        riskScore,
                        riskLevel
                ));

                // Must be inside the loop, after appending a transaction.
                returnedCount++;
            }

                json.append("]");

                byte[] response = json.toString()
                        .getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                        "Content-Type", "application/json; charset=UTF-8"
                );

                exchange.sendResponseHeaders(200, response.length);

                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(response);
                }

            } catch (IOException e) {
                byte[] error = "{\"error\":\"Unable to load transactions\"}"
                        .getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                        "Content-Type", "application/json; charset=UTF-8"
                );

                exchange.sendResponseHeaders(500, error.length);

                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(error);
                }
            }
        });
        
        server.createContext("/", exchange -> {

            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            if (!"/".equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            Path page = Path.of("web", "index.html");

            if (!Files.isRegularFile(page)) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }

            byte[] html = Files.readAllBytes(page);

            exchange.getResponseHeaders().set(
                    "Content-Type", "text/html; charset=UTF-8"
            );

            exchange.sendResponseHeaders(200, html.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(html);
            }
        });

        
        
        server.createContext("/api/investigation", exchange -> {

            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String query = exchange.getRequestURI().getRawQuery();
            String transactionId = null;

            if (query != null) {
                for (String parameter : query.split("&")) {
                    String[] parts = parameter.split("=", 2);

                    if (parts.length == 2 && parts[0].equals("id")) {
                        transactionId = URLDecoder.decode(
                                parts[1], StandardCharsets.UTF_8
                        );
                    }
                }
            }

            if (transactionId == null || !transactionId.matches("TX[1-9][0-9]*")) {
                byte[] error = "{\"error\":\"Invalid transaction ID\"}"
                        .getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                        "Content-Type", "application/json; charset=UTF-8"
                );
                exchange.sendResponseHeaders(400, error.length);

                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(error);
                }
                return;
            }

            try {
                TransactionDataLoader loader = new TransactionDataLoader();

                // Initial investigation scope: first 10,000 CSV records.
                List<Transaction> transactions =
                        loader.loadTransactions("data/transactions.csv", 10000);

                TransactionGraph graph = new TransactionGraph();
                Transaction selected = null;

                // Preserve CSV order and stop at the selected transaction.
                for (Transaction t : transactions) {
                    graph.addTransaction(t);

                    if (t.getTransactionId().equals(transactionId)) {
                        selected = t;
                        break;
                    }
                }

                if (selected == null) {
                    byte[] error = "{\"error\":\"Transaction not found in loaded sample\"}"
                            .getBytes(StandardCharsets.UTF_8);

                    exchange.getResponseHeaders().set(
                            "Content-Type", "application/json; charset=UTF-8"
                    );
                    exchange.sendResponseHeaders(404, error.length);

                    try (OutputStream output = exchange.getResponseBody()) {
                        output.write(error);
                    }
                    return;
                }

        FraudDetectionService fraudService =
                new FraudDetectionService();

        InvestigationReport report =
                new InvestigationReport(selected, fraudService, graph);

        List<Transaction> senderHistory =
                report.getHistoricalTransactions(
                        graph.getOutgoingTransactions(selected.getSender())
                );

        List<Transaction> receiverHistory =
                report.getHistoricalTransactions(
                        graph.getIncomingTransactions(selected.getReceiver())
                );


        java.util.Map<String, Transaction> connectedTransactions =
                new java.util.LinkedHashMap<>();

        String sender = selected.getSender();
        String receiver = selected.getReceiver();

        for (Transaction t : transactions) {

            // Only include transactions encountered up to
            // the selected transaction in CSV order.
            if (t.getTransactionId().equals(selected.getTransactionId())) {
                connectedTransactions.put(t.getTransactionId(), t);
                break;
            }

            if (t.getSender().equals(sender)
                    || t.getReceiver().equals(sender)
                    || t.getSender().equals(receiver)
                    || t.getReceiver().equals(receiver)) {

                connectedTransactions.put(t.getTransactionId(), t);
            }
        }


        StringBuilder json = new StringBuilder();


        json.append("{")
            .append("\"id\":\"").append(escapeJson(selected.getTransactionId())).append("\",")
            .append("\"sender\":\"").append(escapeJson(selected.getSender())).append("\",")
            .append("\"receiver\":\"").append(escapeJson(selected.getReceiver())).append("\",")
            .append("\"amount\":").append(selected.getAmount()).append(",")
            .append("\"riskScore\":").append(fraudService.calculateRiskScore(selected)).append(",")
            .append("\"riskLevel\":\"").append(escapeJson(fraudService.evaluateTransaction(selected))).append("\",");

        json.append("\"explanations\":[");


        List<String> explanations = report.getRiskExplanations();

        for (int i = 0; i < explanations.size(); i++) {
            if (i > 0) {
                json.append(",");
            }

            json.append("\"")
                .append(escapeJson(explanations.get(i)))
                .append("\"");
        }

        
        json.append("],")
            .append("\"senderOutgoingCount\":")
            .append(senderHistory.size()).append(",")
            .append("\"receiverIncomingCount\":")
            .append(receiverHistory.size()).append(",");

        // Combine the sender and receiver histories.
        // Avoid adding the same transaction twice.
        java.util.Map<String, Transaction> uniqueEdges =
        connectedTransactions;

        json.append("\"edges\":[");

        int edgeIndex = 0;

        for (Transaction t : uniqueEdges.values()) {

            if (edgeIndex > 0) {
                json.append(",");
            }

            json.append("{")
                .append("\"id\":\"")
                .append(escapeJson(t.getTransactionId()))
                .append("\",")
                .append("\"source\":\"")
                .append(escapeJson(t.getSender()))
                .append("\",")
                .append("\"target\":\"")
                .append(escapeJson(t.getReceiver()))
                .append("\",")
                .append("\"amount\":")
                .append(t.getAmount())
                .append(",")
                .append("\"step\":")
                .append(t.getStep())
                .append("}");

            edgeIndex++;
        }

        json.append("]}");


        byte[] response =
                json.toString().getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.sendResponseHeaders(200, response.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }

    } catch (IOException e) {
        byte[] error = "{\"error\":\"Unable to load investigation data\"}"
                .getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type", "application/json; charset=UTF-8"
        );
        exchange.sendResponseHeaders(500, error.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(error);
        }
    }
});


        server.start();

        System.out.println(
                "GraphGuard API running at http://127.0.0.1:8080"
        );
    }

    
private static String escapeJson(String value) {
    if (value == null) {
        return "";
    }

    StringBuilder escaped = new StringBuilder();

    for (char c : value.toCharArray()) {
        switch (c) {
            case '"' -> escaped.append("\\\"");
            case '\\' -> escaped.append("\\\\");
            case '\n' -> escaped.append("\\n");
            case '\r' -> escaped.append("\\r");
            case '\t' -> escaped.append("\\t");
            default -> {
                if (c < 0x20) {
                    escaped.append(String.format("\\u%04x", (int) c));
                } else {
                    escaped.append(c);
                }
            }
        }
    }

    return escaped.toString();
}

}
