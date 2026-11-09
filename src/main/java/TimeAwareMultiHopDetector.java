import java.util.List;

public class TimeAwareMultiHopDetector {
    private static final int WINDOW_STEPS = 24;
    private final TransactionGraph graph;
    public TimeAwareMultiHopDetector(TransactionGraph graph) {
        this.graph = graph;
    }

    public boolean hasMultiHopPattern(Transaction outgoingTransfer) {
        if (!"TRANSFER".equals(outgoingTransfer.getType())) {
            return false;
        }
        String middleAccount = outgoingTransfer.getSender();
        String destination = outgoingTransfer.getReceiver();

        List<Transaction> incomingTransfers =
                graph.getIncomingTransactions(middleAccount);

        for (Transaction incoming : incomingTransfers) {

            if(! "TRANSFER".equals(incoming.getType())) {
                continue;
            }

            String origin = incoming.getSender();

            if (origin.equals(middleAccount)
                    || origin.equals(destination)
                    || middleAccount.equals(destination)) {
                continue;
            }

            int timeDifference = 
                 outgoingTransfer.getStep() - incoming.getStep();

            if (timeDifference < 0
                    || timeDifference >= WINDOW_STEPS) {
                continue;
            }

            return true;
        }
        return false;
    }
}