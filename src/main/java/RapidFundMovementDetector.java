
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class RapidFundMovementDetector {

    private static final int WINDOW_STEPS = 24;
    private static final double AMOUNT_RATIO = 0.80;

    private record IncomingTransfer(int step, double amount) {}

    private final Map<String, Deque<IncomingTransfer>> history =
            new HashMap<>();

    public boolean analyze(Transaction transaction) {

        int step = transaction.getStep();
        String type = transaction.getType();

        if ("TRANSFER".equals(type)) {

            Deque<IncomingTransfer> transfers =
                    history.computeIfAbsent(
                            transaction.getReceiver(),
                            key -> new ArrayDeque<>()
                    );

            transfers.addLast(
                    new IncomingTransfer(
                            step,
                            transaction.getAmount()
                    )
            );

            return false;
        }

        if ("CASH_OUT".equals(type)) {

            Deque<IncomingTransfer> transfers =
                    history.get(transaction.getSender());

            if (transfers == null) {
                return false;
            }

            // Remove expired incoming transfers.
            while (!transfers.isEmpty()
                    && transfers.peekFirst().step()
                            <= step - WINDOW_STEPS) {

                transfers.removeFirst();
            }

            // Check whether cash-out closely matches
            // a recent incoming transfer.
            
            
            for (java.util.Iterator<IncomingTransfer> iterator =
                    transfers.iterator(); iterator.hasNext();) {

                IncomingTransfer transfer = iterator.next();

                int timeDifference = step - transfer.step();

                boolean validTime =
                        timeDifference >= 0
                        && timeDifference < WINDOW_STEPS;

                boolean validAmount =
                        transfer.amount() > 0
                        && transaction.getAmount()
                                >= transfer.amount() * AMOUNT_RATIO
                        && transaction.getAmount()
                                <= transfer.amount();

                if (validTime && validAmount) {
                    iterator.remove();
                    return true;
                }
            }


        }

        return false;
    }
}
