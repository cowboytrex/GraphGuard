
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectedNetworkTest {

    public static void main(String[] args) throws Exception {

        TransactionDataLoader loader = new TransactionDataLoader();

        List<Transaction> transactions =
                loader.loadTransactions("data/transactions.csv", 10000);

        Map<String, Integer> accountCounts = new HashMap<>();

        for (Transaction t : transactions) {

            accountCounts.merge(t.getSender(), 1, Integer::sum);
            accountCounts.merge(t.getReceiver(), 1, Integer::sum);
        }

        String selectedAccount = null;
        int highestCount = 0;

        for (Map.Entry<String, Integer> entry : accountCounts.entrySet()) {

            if (entry.getValue() > highestCount) {
                selectedAccount = entry.getKey();
                highestCount = entry.getValue();
            }
        }

        System.out.println("Most connected account: " + selectedAccount);
        System.out.println("Transaction count: " + highestCount);

        if (highestCount <= 1) {
            System.out.println("No repeated accounts found in sample.");
            return;
        }

        for (Transaction t : transactions) {

            if (t.getSender().equals(selectedAccount)
                    || t.getReceiver().equals(selectedAccount)) {

                System.out.printf(
                        "%s | Step %d | %s -> %s | $%,.2f%n",
                        t.getTransactionId(),
                        t.getStep(),
                        t.getSender(),
                        t.getReceiver(),
                        t.getAmount()
                );
            }
        }
    }
}
