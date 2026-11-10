
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphFraudDetectionService {

    public Set<String> detectIntermediaries(
            List<Transaction> transactions) {

        // Copy transactions to avoid modifying the original list
        List<Transaction> sortedTransactions =
                new ArrayList<>(transactions);

        // Sort transactions chronologically by PaySim step
        sortedTransactions.sort(
                Comparator.comparingInt(Transaction::getStep)
        );

        Map<String, Integer> firstReceivedStep = new HashMap<>();
        Set<String> intermediaries = new LinkedHashSet<>();

        for (Transaction t : sortedTransactions) {

            // Only analyze transfers
            if (!"TRANSFER".equals(t.getType())) {
                continue;
            }

            String sender = t.getSender();
            String receiver = t.getReceiver();
            int currentStep = t.getStep();

            // Check whether the sender received funds earlier
            Integer receivedStep = firstReceivedStep.get(sender);

            if (receivedStep != null && currentStep > receivedStep) {
                intermediaries.add(sender);
            }

            // Record the earliest incoming transfer
            firstReceivedStep.putIfAbsent(receiver, currentStep);
        }

        return intermediaries;
    }
}
