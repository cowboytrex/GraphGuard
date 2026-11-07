
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        TransactionDataLoader loader = new TransactionDataLoader();
        FraudDetectionService detector = new FraudDetectionService();

        try {
            List<Transaction> transactions =
                    loader.loadTransactions("data/transactions.csv", 100);

            System.out.println("Loaded: " + transactions.size());

            for (Transaction transaction : transactions) {

                String risk = detector.evaluateTransaction(transaction);

                System.out.println(
                    transaction.getTransactionId()
                    + " | Amount: " + transaction.getAmount()
                    + " | Risk: " + risk
                );
            }

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
