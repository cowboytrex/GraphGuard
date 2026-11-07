
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        TransactionDataLoader loader = new TransactionDataLoader();
        FraudDetectionService detector = new FraudDetectionService();
        FraudEvaluator evaluator = new FraudEvaluator();

        String filePath = "data/transactions.csv";

        // Chronological held-out evaluation
        int testStartStep = 501;
        int testEndStep = 743;

        try {
            System.out.println("=== GraphGuard Evaluation ===");
            System.out.println(
                    "Held-out period: steps "
                    + testStartStep + "-" + testEndStep
            );

            List<Transaction> testTransactions =
                    loader.loadTransactionsByStep(
                            filePath,
                            testStartStep,
                            testEndStep
                    );

            System.out.println(
                    "Loaded test transactions: "
                    + testTransactions.size()
            );

            evaluator.evaluate(testTransactions, detector);

        } catch (IOException e) {
            System.err.println(
                    "Error loading transactions: " + e.getMessage()
            );
        }
    }
}
