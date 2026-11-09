
import java.util.List;

public class InvestigationReportTest {

    public static void main(String[] args) {

        Transaction transaction = new Transaction(
                "TX-001",
                "ACCOUNT-A",
                "ACCOUNT-B",
                50000,
                true,
                "TRANSFER",
                50000,
                0,
                10
        );

        FraudDetectionService service =
                new FraudDetectionService();

        TransactionGraph graph = new TransactionGraph();
        graph.addTransaction(transaction);

        
        graph.addTransaction(new Transaction(
                "TX-002",
                "ACCOUNT-A",
                "ACCOUNT-C",
                12000,
                false,
                "TRANSFER",
                62000,
                50000,
                8
        ));

        graph.addTransaction(new Transaction(
                "TX-003",
                "ACCOUNT-E",
                "ACCOUNT-B",
                5000,
                false,
                "TRANSFER",
                5000,
                0,
                7
        ));


        InvestigationReport report =
                new InvestigationReport(
                        transaction, service, graph
                );

        List<String> explanations =
                report.getRiskExplanations();

        if (explanations.size() != 3) {
            throw new AssertionError(
                    "Expected 3 explanations, got "
                    + explanations.size()
            );
        }

        if (service.calculateRiskScore(transaction) != 75) {
            throw new AssertionError(
                    "Expected risk score 75"
            );
        }

        if (!"HIGH RISK".equals(
                service.evaluateTransaction(transaction))) {
            throw new AssertionError(
                    "Expected HIGH RISK"
            );
        }

        System.out.println(
                "PASS: Investigation explanations"
        );

        System.out.println(
                "PASS: Risk score calculation"
        );

        System.out.println(
                "PASS: Risk classification"
        );

        graph.addTransaction(new Transaction(
        "TX-FUTURE",
        "ACCOUNT-A",
        "ACCOUNT-Z",
        9000,
        false,
        "TRANSFER",
        9000,
        0,
        20
        ));

        
        int currentStep = transaction.getStep();

        long outgoingCount = graph
                .getOutgoingTransactions(transaction.getSender())
                .stream()
                .filter(t -> t.getStep() <= currentStep)
                .count();

        long incomingCount = graph
                .getIncomingTransactions(transaction.getReceiver())
                .stream()
                .filter(t -> t.getStep() <= currentStep)
                .count();

        if (outgoingCount != 2) {
            throw new AssertionError(
                    "Expected 2 historical outgoing transactions"
            );
        }

        if (incomingCount != 2) {
            throw new AssertionError(
                    "Expected 2 historical incoming transactions"
            );
        }

        System.out.println("PASS: Time-aware graph counts");


        report.printReport();

        System.out.println(
                "\nAll investigation report tests passed."
        );
    }
}
