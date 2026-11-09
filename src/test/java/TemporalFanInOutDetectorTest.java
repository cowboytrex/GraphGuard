
public class TemporalFanInOutDetectorTest {

    private static Transaction transaction(
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
            int expected,
            int actual) {

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

        graph.addTransaction(transaction("A", "D", 10));
        graph.addTransaction(transaction("B", "D", 11));
        graph.addTransaction(transaction("A", "D", 12));
        graph.addTransaction(transaction("C", "D", 13));

        graph.addTransaction(transaction("D", "E", 14));
        graph.addTransaction(transaction("D", "F", 15));
        graph.addTransaction(transaction("D", "E", 16));

        TemporalFanInOutDetector detector =
                new TemporalFanInOutDetector(graph);

        check(
                "Distinct fan-in",
                3,
                detector.countRecentDistinctSenders("D", 16)
        );

        check(
                "Distinct fan-out",
                2,
                detector.countRecentDistinctReceivers("D", 16)
        );

        check(
                "Old transactions excluded",
                0,
                detector.countRecentDistinctSenders("D", 40)
        );

        check(
                "Future transactions excluded",
                0,
                detector.countRecentDistinctReceivers("D", 9)
        );

        check(
                "Unknown account",
                0,
                detector.countRecentDistinctSenders("UNKNOWN", 16)
        );

        System.out.println("All temporal fan-in/out tests passed.");
    }
}
