import java.util.HashSet;
import java.util.Set;

public class GraphFraudDetector {

    private final TransactionGraph graph;

    public GraphFraudDetector(TransactionGraph graph) {
        this.graph = graph;
    }

    // Count unique accounts sending money to a receiver
    public int countDistinctSenders(String receiver) {

        Set<String> senders = new HashSet<>();

        for (Transaction transaction :
                graph.getIncomingTransactions(receiver)) {

            senders.add(transaction.getSender());
        }

        return senders.size();
    }

    // Flag receivers with many distinct senders
    public boolean hasFanInPattern(
            String receiver,
            int minimumSenders
    ) {
        if (minimumSenders < 2) {
            throw new IllegalArgumentException(
                    "Minimum senders must be at least 2"
            );
        }

        return countDistinctSenders(receiver) >= minimumSenders;
    }
}