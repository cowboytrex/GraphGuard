
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class IntermediaryAccountTest {

    public static void main(String[] args) throws Exception {

        TransactionDataLoader loader = new TransactionDataLoader();

        List<Transaction> transactions =
                loader.loadTransactions("data/transactions.csv", 100000);


        Set<String> senders = new HashSet<>();
        Set<String> receivers = new HashSet<>();

        for (Transaction t : transactions) {
            senders.add(t.getSender());
            receivers.add(t.getReceiver());
        }

        Set<String> overlap = new HashSet<>(senders);
        overlap.retainAll(receivers);

        System.out.println("Unique senders: " + senders.size());
        System.out.println("Unique receivers: " + receivers.size());
        System.out.println("Accounts appearing as both: " + overlap.size());


        java.util.Map<String, Integer> typeCounts =
                new java.util.TreeMap<>();

        for (Transaction t : transactions) {
            typeCounts.merge(t.getType(), 1, Integer::sum);
        }

        System.out.println("Transaction types:");
        typeCounts.forEach((type, count) ->
                System.out.println(type + ": " + count)
        );


        Set<String> receivedBefore = new HashSet<>();
        Set<String> intermediaries = new LinkedHashSet<>();

        for (Transaction t : transactions) {

            String sender = t.getSender();
            String receiver = t.getReceiver();

            if (receivedBefore.contains(sender)) {
                intermediaries.add(sender);
            }

            receivedBefore.add(receiver);
        }

        System.out.println(
                "Potential intermediary accounts: "
                + intermediaries.size()
        );

        intermediaries.stream()
                .limit(10)
                .forEach(System.out::println);
    }
}
