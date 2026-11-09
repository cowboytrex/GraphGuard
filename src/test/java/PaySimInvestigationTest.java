import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.List;

public class PaySimInvestigationTest {

    public static void main(String[] args) throws IOException {

        TransactionDataLoader loader =
                new TransactionDataLoader();

        FraudDetectionService fraudService =
                new FraudDetectionService();

        TransactionGraph graph =
                new TransactionGraph();

        List<Transaction> transactions =
                loader.loadTransactions(
                        "data/transactions.csv", 1000
                );

        if (transactions.size() != 1000) {
            throw new AssertionError(
                    "Expected 1000 transactions"
            );
        }

        System.out.println("PASS: PaySim transactions loaded");

        int highRiskCount = 0;
        int truePositiveCount = 0;

        for (Transaction transaction : transactions) {

            graph.addTransaction(transaction);

            if ("HIGH RISK".equals(
                    fraudService.evaluateTransaction(transaction))) {

                highRiskCount++;

                if (transaction.isFraud()) {
                    truePositiveCount++;
                }
            }
        }

        if (highRiskCount == 0) {
            throw new AssertionError(
                    "Expected at least one high-risk transaction"
            );
        }

        if (truePositiveCount == 0) {
            throw new AssertionError(
                    "Expected at least one true positive"
            );
        }

        System.out.println("PASS: High-risk transactions detected");
        System.out.println("PASS: Fraud labels verified");

        Transaction first = transactions.get(0);

        InvestigationReport report =
                new InvestigationReport(
                        first,
                        fraudService,
                        graph
                );

        if (report.getRiskExplanations() == null) {
            throw new AssertionError(
                    "Risk explanations must not be null"
            );
        }

        System.out.println("PASS: Investigation report created");

        
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            System.setOut(new PrintStream(
                    output, true, StandardCharsets.UTF_8
            ));

            report.printReport();

        } finally {
            System.setOut(originalOut);
        }

        String reportText = output.toString(StandardCharsets.UTF_8);

        if (!reportText.contains(
                "Transaction ID: " + first.getTransactionId())) {
            throw new AssertionError("Transaction ID missing from report");
        }

        if (!reportText.contains(
                "Sender: " + first.getSender())) {
            throw new AssertionError("Sender missing from report");
        }

        if (!reportText.contains(
                "Receiver: " + first.getReceiver())) {
            throw new AssertionError("Receiver missing from report");
        }

        if (!reportText.contains("Risk Score: ")) {
            throw new AssertionError("Risk score missing from report");
        }

        if (!reportText.contains("Sender Outgoing History")) {
            throw new AssertionError("Sender history missing from report");
        }

        if (!reportText.contains("Receiver Incoming History")) {
            throw new AssertionError("Receiver history missing from report");
        }

        System.out.println("PASS: Investigation report output verified");


        System.out.println(
                "\nHigh-risk transactions: " + highRiskCount
        );

        System.out.println(
                "True positives: " + truePositiveCount
        );

        System.out.println(
                "\nAll PaySim integration tests passed."
        );
    }
}
