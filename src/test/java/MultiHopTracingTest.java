
import java.util.ArrayList;
import java.util.List;

public class MultiHopTracingTest {

    public static void main(String[] args) {

        List<Transaction> transactions = new ArrayList<>();

        // A -> B at step 1
        transactions.add(new Transaction(
                "MH001", "AccountA", "AccountB",
                1000.0, false, "TRANSFER",
                2000.0, 1000.0, 1
        ));

        // B -> C at step 2
        transactions.add(new Transaction(
                "MH002", "AccountB", "AccountC",
                950.0, false, "TRANSFER",
                1000.0, 50.0, 2
        ));

        // C -> D at step 3
        transactions.add(new Transaction(
                "MH003", "AccountC", "AccountD",
                900.0, false, "TRANSFER",
                950.0, 50.0, 3
        ));

        System.out.println("Transactions loaded: "
                + transactions.size());

        for (Transaction t : transactions) {
            System.out.println(
                    t.getSender() + " -> " + t.getReceiver()
                    + " | Amount: $" + t.getAmount()
                    + " | Step: " + t.getStep()
            );
        }

        //test 1:discover mutil-hop transaction paths
        MultiHopTracer tracer = new MultiHopTracer();

        List<List<Transaction>> paths = 
                tracer.trace(transactions, "AccountA", 3);

        System.out.print("\nDiscovered paths: " + paths.size());

        for (List<Transaction> path : paths) {
            System.out.print(path.get(0).getSender());

            for(Transaction t : path) {
                System.out.print(" -> " + t.getReceiver());
            }

            System.out.println(" | Hops: " + path.size());
        }

        //verify the complete three three hop path exists
        boolean foundThreeHopPath = false;

        for(List<Transaction> path : paths) {
            if (path.size() == 3
                && path.get(2).getReceiver().equals("AccountD")) {
                    foundThreeHopPath = true;
                }
        }

        if (!foundThreeHopPath) {
            throw new AssertionError(
                    "TEST1 FAILED: three hop path not found"
            );
        }

        System.out.println("TEST1 PASSED: three hop path discovered");
        
        // TEST 2: Reject backward-in-time paths
        List<Transaction> wrongOrder = new ArrayList<>();

        wrongOrder.add(new Transaction(
                "CH001", "AccountA", "AccountB",
                1000.0, false, "TRANSFER",
                2000.0, 1000.0, 5
        ));

        wrongOrder.add(new Transaction(
                "CH002", "AccountB", "AccountC",
                950.0, false, "TRANSFER",
                1000.0, 50.0, 2
        ));

        List<List<Transaction>> wrongOrderPaths =
                tracer.trace(wrongOrder, "AccountA", 3);

        for (List<Transaction> path : wrongOrderPaths) {
            if (path.size() > 1) {
                throw new AssertionError(
                        "TEST 2 FAILED: Backward-in-time path detected"
                );
            }
        }

        System.out.println("TEST 2 PASSED: Backward-in-time paths rejected");

        
        // TEST 3: Prevent cycles
        List<Transaction> cycleTransactions = new ArrayList<>();

        cycleTransactions.add(new Transaction(
                "CY001", "AccountA", "AccountB",
                1000.0, false, "TRANSFER",
                2000.0, 1000.0, 1
        ));

        cycleTransactions.add(new Transaction(
                "CY002", "AccountB", "AccountC",
                950.0, false, "TRANSFER",
                1000.0, 50.0, 2
        ));

        cycleTransactions.add(new Transaction(
                "CY003", "AccountC", "AccountA",
                900.0, false, "TRANSFER",
                950.0, 50.0, 3
        ));

        List<List<Transaction>> cyclePaths =
                tracer.trace(cycleTransactions, "AccountA", 5);

        if (cyclePaths.size() != 2) {
            throw new AssertionError(
                    "TEST 3 FAILED: Expected 2 paths, found "
                    + cyclePaths.size()
            );
        }

        System.out.println("TEST 3 PASSED: Cycles prevented");

        
        // TEST 4: Enforce maximum-hop limit
        List<List<Transaction>> limitedPaths =
                tracer.trace(transactions, "AccountA", 2);

        if (limitedPaths.size() != 2) {
            throw new AssertionError(
                    "TEST 4 FAILED: Expected 2 paths, found "
                    + limitedPaths.size()
            );
        }

        for (List<Transaction> path : limitedPaths) {
            if (path.size() > 2) {
                throw new AssertionError(
                        "TEST 4 FAILED: Maximum-hop limit exceeded"
                );
            }
        }

        System.out.println(
                "TEST 4 PASSED: Maximum-hop limit enforced"
        );




    }
}
