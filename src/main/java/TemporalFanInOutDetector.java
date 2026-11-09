
import java.util.HashSet;
import java.util.Set;

public class TemporalFanInOutDetector {

    private static final int WINDOW_STEPS = 24;

    private final TransactionGraph graph;

    public TemporalFanInOutDetector(TransactionGraph graph) {
        this.graph = graph;
    }

    public int countRecentDistinctSenders(
            String account,
            int currentStep) {

        Set<String> senders = new HashSet<>();

        for (Transaction transaction :
                graph.getIncomingTransactions(account)) {

            int difference =
                    currentStep - transaction.getStep();

            if (difference >= 0
                    && difference < WINDOW_STEPS) {

                senders.add(transaction.getSender());
            }
        }

        return senders.size();
    }

    public int countRecentDistinctReceivers(
            String account,
            int currentStep) {

        Set<String> receivers = new HashSet<>();

        for (Transaction transaction :
                graph.getOutgoingTransactions(account)) {

            int difference =
                    currentStep - transaction.getStep();

            if (difference >= 0
                    && difference < WINDOW_STEPS) {

                receivers.add(transaction.getReceiver());
            }
        }

        return receivers.size();
    }

    public boolean hasHighFanIn(
            String account,
            int currentStep,
            int threshold) {

        return countRecentDistinctSenders(
                account, currentStep) >= threshold;
    }

    public boolean hasHighFanOut(
            String account,
            int currentStep,
            int threshold) {

        return countRecentDistinctReceivers(
                account, currentStep) >= threshold;
    }
}
