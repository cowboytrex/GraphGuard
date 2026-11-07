
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GraphFraudDetectorTest {

    private Transaction createTransaction(
            String id,
            String sender,
            String receiver
    ) {
        return new Transaction(
                id,
                sender,
                receiver,
                100.0,
                false,
                "TRANSFER",
                500.0,
                400.0,
                1
        );
    }

    @Test
    void testFanInDetected() {
        TransactionGraph graph = new TransactionGraph();

        graph.addTransaction(createTransaction("T1", "A", "D"));
        graph.addTransaction(createTransaction("T2", "B", "D"));
        graph.addTransaction(createTransaction("T3", "C", "D"));

        GraphFraudDetector detector = new GraphFraudDetector(graph);

        assertEquals(3, detector.countDistinctSenders("D"));
        assertTrue(detector.hasFanInPattern("D", 3));
    }

    @Test
    void testRepeatedSenderNotDoubleCounted() {
        TransactionGraph graph = new TransactionGraph();

        graph.addTransaction(createTransaction("T1", "A", "D"));
        graph.addTransaction(createTransaction("T2", "A", "D"));
        graph.addTransaction(createTransaction("T3", "B", "D"));

        GraphFraudDetector detector = new GraphFraudDetector(graph);

        assertEquals(2, detector.countDistinctSenders("D"));
        assertFalse(detector.hasFanInPattern("D", 3));
    }

    @Test
    void testUnknownReceiver() {
        TransactionGraph graph = new TransactionGraph();
        GraphFraudDetector detector = new GraphFraudDetector(graph);

        assertEquals(0, detector.countDistinctSenders("Unknown"));
        assertFalse(detector.hasFanInPattern("Unknown", 3));
    }

    @Test
    void testInvalidThreshold() {
        TransactionGraph graph = new TransactionGraph();
        GraphFraudDetector detector = new GraphFraudDetector(graph);

        assertThrows(
                IllegalArgumentException.class,
                () -> detector.hasFanInPattern("D", 1)
        );
    }
}
