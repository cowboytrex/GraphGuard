
public class TimeAwareMultiHopDetectorTest {

    private static Transaction transfer(
            String sender,
            String receiver,
            int step) {

        return new Transaction(
                "TEST-" + sender + "-" + receiver,
                sender,
                receiver,
                1000,
                false,
                "TRANSFER",
                1000,
                0,
                step
        );
    }

    private static void check(
            String name,
            boolean expected,
            boolean actual) {

        if (expected != actual) {
            throw new AssertionError(
                    name + ": expected " + expected
                            + ", got " + actual
            );
        }

        System.out.println("PASS: " + name);
    }

    public static void main(String[] args) {

        TransactionGraph graph = new TransactionGraph();
        TimeAwareMultiHopDetector detector =
                new TimeAwareMultiHopDetector(graph);

        graph.addTransaction(transfer("A", "B", 10));

        check(
                "Valid two-hop chain",
                true,
                detector.hasMultiHopPattern(
                        transfer("B", "C", 12))
        );

        check(
                "Outside time window",
                false,
                detector.hasMultiHopPattern(
                        transfer("B", "C", 35))
        );

        check(
                "Wrong middle account",
                false,
                detector.hasMultiHopPattern(
                        transfer("D", "C", 12))
        );

        check(
                "Repeated origin and destination",
                false,
                detector.hasMultiHopPattern(
                        transfer("B", "A", 12))
        );

        check(
                "Outgoing transfer before incoming",
                false,
                detector.hasMultiHopPattern(
                        transfer("B", "C", 8))
        );

        System.out.println("All multi-hop tests passed.");
    }
}
