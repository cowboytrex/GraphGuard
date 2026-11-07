
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionGraph {

    private final Map<String, List<Transaction>> outgoing =
            new HashMap<>();

    private final Map<String, List<Transaction>> incoming =
            new HashMap<>();

    public void addTransaction(Transaction transaction) {

        outgoing.computeIfAbsent(
                transaction.getSender(),
                key -> new ArrayList<>()
        ).add(transaction);

        incoming.computeIfAbsent(
                transaction.getReceiver(),
                key -> new ArrayList<>()
        ).add(transaction);
    }

    public List<Transaction> getOutgoingTransactions(String account) {
        return outgoing.getOrDefault(account, List.of());
    }

    public List<Transaction> getIncomingTransactions(String account) {
        return incoming.getOrDefault(account, List.of());
    }

    public int getOutgoingTransactionCount(String account) {
        return getOutgoingTransactions(account).size();
    }

    public int getIncomingTransactionCount(String account) {
        return getIncomingTransactions(account).size();
    }

    public int getSenderCount() {
        return outgoing.size();
    }

    public int getReceiverCount() {
        return incoming.size();
    }
}
