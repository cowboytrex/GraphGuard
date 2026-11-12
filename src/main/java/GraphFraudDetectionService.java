
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

    
    public Set<String> detectHighValueForwarding(
            List<Transaction> transactions) {

        List<Transaction> sorted = new ArrayList<>(transactions);
        sorted.sort(Comparator.comparingInt(Transaction::getStep));

        Map<String, List<Transaction>> incoming = new HashMap<>();
        Set<String> flaggedAccounts = new LinkedHashSet<>();

        for (Transaction current : sorted) {

            if (!"TRANSFER".equals(current.getType())
                    || current.getAmount() <= 0) {
                continue;
            }

            String sender = current.getSender();

            List<Transaction> previousIncoming =
                    incoming.getOrDefault(sender, List.of());

            for (Transaction previous : previousIncoming) {

                int stepDifference =
                        current.getStep() - previous.getStep();

                if (stepDifference <= 0 || stepDifference > 3) {
                    continue;
                }

                double ratio =
                        current.getAmount() / previous.getAmount();

                if (ratio >= 0.80 && ratio <= 1.00
                        && !previous.getSender().equals(current.getReceiver())) {

                    flaggedAccounts.add(sender);
                    break;
                }
            }

            incoming.computeIfAbsent(
                    current.getReceiver(),
                    key -> new ArrayList<>()
            ).add(current);
        }

        return flaggedAccounts;
    }

}
