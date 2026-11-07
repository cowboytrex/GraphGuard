
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class GraphAnalyzer {

    public static void main(String[] args) {

        TransactionDataLoader loader = new TransactionDataLoader();

        try {
            List<Transaction> transactions =
                    loader.loadTransactions(
                            "data/transactions.csv",
                            100000
                    );

            TransactionGraph graph = new TransactionGraph();

            for (Transaction transaction : transactions) {
                graph.addTransaction(transaction);
            }

            GraphFraudDetector detector =
                    new GraphFraudDetector(graph);

            Map<String, Integer> receiverCounts = new HashMap<>();

            for (Transaction transaction : transactions) {
                String receiver = transaction.getReceiver();

                receiverCounts.put(
                        receiver,
                        detector.countDistinctSenders(receiver)
                );
            }

            int fanInAccounts = 0;
            int threshold = 3;

            for (Map.Entry<String, Integer> entry :
                    receiverCounts.entrySet()) {

                if (entry.getValue() >= threshold) {
                    fanInAccounts++;
                }
            }

            System.out.println("=== GraphGuard Graph Analysis ===");
            System.out.println(
                    "Transactions loaded: " + transactions.size()
            );
            System.out.println(
                    "Unique senders: " + graph.getSenderCount()
            );
            System.out.println(
                    "Unique receivers: " + graph.getReceiverCount()
            );
            System.out.println(
                    "Receivers with 3+ distinct senders: "
                    + fanInAccounts
            );

            System.out.println("\n=== Top 10 Receivers ===");

            receiverCounts.entrySet()
                    .stream()
                    .sorted(
                            Map.Entry.<String, Integer>
                                    comparingByValue().reversed()
                    )
                    .limit(10)
                    .forEach(entry ->
                            System.out.println(
                                    entry.getKey()
                                    + " | Distinct senders: "
                                    + entry.getValue()
                            )
                    );

        } catch (IOException e) {
            System.err.println(
                    "Error loading transactions: " + e.getMessage()
            );
        }
    }
}
