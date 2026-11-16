
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SyntheticIntermediaryTest {

    public static void main(String[] args) {

        List<Transaction> transactions = new ArrayList<>();

        // Step 1: A sends $1,000 to B
        transactions.add(new Transaction(
                "TEST001",
                "AccountA",
                "AccountB",
                1000.0,
                false,
                "TRANSFER",
                2000.0,
                1000.0,
                1
        ));

        // Step 2: B sends $950 to C
        transactions.add(new Transaction(
                "TEST002",
                "AccountB",
                "AccountC",
                950.0,
                false,
                "TRANSFER",
                1000.0,
                50.0,
                2
        ));

        
        GraphFraudDetectionService detector =
                new GraphFraudDetectionService();

        Set<String> intermediaries =
                detector.detectIntermediaries(transactions);


        System.out.println("Intermediary accounts detected: "
                + intermediaries.size());

        intermediaries.forEach(System.out::println);

        if (!intermediaries.contains("AccountB")
                || intermediaries.size() != 1) {
            throw new AssertionError(
                    "Expected exactly one intermediary: AccountB"
            );
        }

        System.out.println("TEST PASSED");

        
        // Negative test: both transfers occur in the same step
        List<Transaction> sameStepTransactions = new ArrayList<>();

        sameStepTransactions.add(new Transaction(
                "TEST003",
                "AccountA",
                "AccountB",
                1000.0,
                false,
                "TRANSFER",
                2000.0,
                1000.0,
                1
        ));

        sameStepTransactions.add(new Transaction(
                "TEST004",
                "AccountB",
                "AccountC",
                950.0,
                false,
                "TRANSFER",
                1000.0,
                50.0,
                1
        ));

        Set<String> sameStepResults =
                detector.detectIntermediaries(sameStepTransactions);

        if (!sameStepResults.isEmpty()) {
            throw new AssertionError(
                    "Same-step transfers must not trigger intermediary detection"
            );
        }

        System.out.println("Same-step negative test passed");
        
        
        // Test: transactions supplied in reverse order
        List<Transaction> reversedTransactions =
                new ArrayList<>(transactions);

        java.util.Collections.reverse(reversedTransactions);

        Set<String> reversedResults =
                detector.detectIntermediaries(reversedTransactions);

        if (!reversedResults.contains("AccountB")) {
            throw new AssertionError(
                    "Reverse-order test failed: AccountB was not detected"
            );
        }

        System.out.println("Reverse-order test passed");

    }
}
