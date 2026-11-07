
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TransactionGraphTest {

    private Transaction createTransaction(
            String id,
            String sender,
            String receiver,
            double amount
    ) {
        return new Transaction(
                id,
                sender,
                receiver,
                amount,
                false,
                "TRANSFER",
                1000.0,
                1000.0 - amount,
                1
        );
    }

    @Test
    void testAddTransaction() {
        TransactionGraph graph = new TransactionGraph();

        graph.addTransaction(
                createTransaction("T1", "Alice", "Bob", 100.0)
        );

        assertEquals(1, graph.getOutgoingTransactionCount("Alice"));
        assertEquals(1, graph.getSenderCount());
    }

    @Test
    void testMultipleTransactionsFromSameSender() {
        TransactionGraph graph = new TransactionGraph();

        graph.addTransaction(
                createTransaction("T1", "Alice", "Bob", 100.0)
        );
        graph.addTransaction(
                createTransaction("T2", "Alice", "Charlie", 200.0)
        );

        assertEquals(2, graph.getOutgoingTransactionCount("Alice"));
        assertEquals(1, graph.getSenderCount());
    }

    @Test
    void testDifferentSenders() {
        TransactionGraph graph = new TransactionGraph();

        graph.addTransaction(
                createTransaction("T1", "Alice", "Bob", 100.0)
        );
        graph.addTransaction(
                createTransaction("T2", "Charlie", "Bob", 200.0)
        );

        assertEquals(2, graph.getSenderCount());
        assertEquals(1, graph.getOutgoingTransactionCount("Alice"));
        assertEquals(1, graph.getOutgoingTransactionCount("Charlie"));
    }

    @Test
    void testUnknownAccount() {
        TransactionGraph graph = new TransactionGraph();

        assertEquals(0, graph.getOutgoingTransactionCount("Unknown"));
        assertTrue(graph.getOutgoingTransactions("Unknown").isEmpty());
    }

    
    @Test
    void testIncomingTransactions() {
        TransactionGraph graph = new TransactionGraph();

        graph.addTransaction(
                createTransaction("T1", "Alice", "Bob", 100.0)
        );

        graph.addTransaction(
                createTransaction("T2", "Charlie", "Bob", 200.0)
        );

        assertEquals(2, graph.getIncomingTransactionCount("Bob"));
        assertEquals(1, graph.getReceiverCount());
    }

    @Test
    void testUnknownReceiver() {
        TransactionGraph graph = new TransactionGraph();

        assertEquals(0, graph.getIncomingTransactionCount("Unknown"));
        assertTrue(graph.getIncomingTransactions("Unknown").isEmpty());
    }

}
