
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        TransactionDataLoader loader = new TransactionDataLoader();
        FraudDetectionService detector = new FraudDetectionService();

        try {
            List<Transaction> transactions =
                    loader.loadTransactions("data/transactions.csv", 1000000);

            System.out.println("Loaded: " + transactions.size());

            FraudEvaluator evaluator = new FraudEvaluator();
            evaluator.evaluate(transactions, detector);

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
