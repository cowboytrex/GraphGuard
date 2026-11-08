
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TimeAwareFanInAnalyzer {

    private static final int WINDOW_STEPS = 24;
    private static final int FAN_IN_THRESHOLD = 3;

    private record IncomingTransfer(int step, String sender) {}

    private final Map<String, Deque<IncomingTransfer>> history =
            new HashMap<>();

    public int recordTransfer(int step, String sender, String receiver) {

        Deque<IncomingTransfer> transfers =
                history.computeIfAbsent(
                        receiver,
                        key -> new ArrayDeque<>()
                );

        // Remove transfers outside the time window
        while (!transfers.isEmpty()
                && transfers.peekFirst().step()
                        <= step - WINDOW_STEPS) {
            transfers.removeFirst();
        }

        transfers.addLast(new IncomingTransfer(step, sender));

        Set<String> uniqueSenders = new HashSet<>();

        for (IncomingTransfer transfer : transfers) {
            uniqueSenders.add(transfer.sender());
        }

        return uniqueSenders.size();
    }

    public boolean isSuspicious(
            int step,
            String sender,
            String receiver
    ) {
        return recordTransfer(step, sender, receiver)
                >= FAN_IN_THRESHOLD;
    }

    public static void main(String[] args) {

        TimeAwareFanInAnalyzer analyzer =
                new TimeAwareFanInAnalyzer();

        System.out.println(
                analyzer.isSuspicious(10, "A", "X")
        ); // false

        System.out.println(
                analyzer.isSuspicious(15, "B", "X")
        ); // false

        System.out.println(
                analyzer.isSuspicious(20, "C", "X")
        ); // true

        System.out.println(
                analyzer.isSuspicious(40, "D", "X")
        ); // false
    }
}
