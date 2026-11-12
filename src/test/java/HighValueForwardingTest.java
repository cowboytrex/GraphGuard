import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HighValueForwardingTest {
    public static void main(String[] args) {
        GraphFraudDetectionService detector = new GraphFraudDetectionService();
        
        //test 1: 95% forwarding
        List<Transaction> highValue = new ArrayList<>();

        highValue.add(new Transaction(
            "HV001", "AccountA", "AccountB",
            1000.0, false, "TRANSFER", 
            2000.0, 1000.0, 1
        ));

        highValue.add(new Transaction(
            "HV002", "AccountB", "AccountC",
            950.0, false, "TRANSFER",
            1000.0, 50.0, 2
        ));

        Set<String> highValueResults = 
            detector.detectHighValueForwarding(highValue);

        if (!highValueResults.contains("AccountB")) {
            throw new AssertionError(
                "TEST 1 FAILED: AcountB should be detedcted"
            );
        }

        System.out.println("TEST 1 PASSED: High value forwarding detected");


        //TEST 2: 20% forwarding
        List<Transaction> lowValue = new ArrayList<>();

        lowValue.add(new Transaction(
            "LV001", "AccountX", "AccountY",
            1000.0, false, "TRANSFER",
            2000.0, 1000.0, 1
        ));

        lowValue.add(new Transaction(
            "LV002", "AccountY", "AccountZ",
            200.0, false, "TRANSFER",
            1000.0, 800.0, 2
        ));

        Set<String> lowValueAccounts = 
            detector.detectHighValueForwarding(lowValue);

        if (!lowValueAccounts.isEmpty()) {
            throw new AssertionError(
                "TEST 2 FAILED: Low value forwarding should not be detected"
            );
        }
        System.out.println("TEST 2 PASSED: Low value forwarding ignored");
        
        
        // TEST 3: Forwarding within the time window
        List<Transaction> withinWindow = new ArrayList<>();

        withinWindow.add(new Transaction(
                "TW001", "AccountA", "AccountB",
                1000.0, false, "TRANSFER",
                2000.0, 1000.0, 1
        ));

        withinWindow.add(new Transaction(
                "TW002", "AccountB", "AccountC",
                950.0, false, "TRANSFER",
                1000.0, 50.0, 3
        ));

        if (!detector.detectHighValueForwarding(withinWindow)
                .contains("AccountB")) {
            throw new AssertionError(
                    "TEST 3 FAILED: Transfer within window not detected"
            );
        }

        System.out.println("TEST 3 PASSED: Within-window forwarding detected");

        // TEST 4: Forwarding outside the time window
        List<Transaction> outsideWindow = new ArrayList<>();

        outsideWindow.add(new Transaction(
                "TW003", "AccountA", "AccountB",
                1000.0, false, "TRANSFER",
                2000.0, 1000.0, 1
        ));

        outsideWindow.add(new Transaction(
                "TW004", "AccountB", "AccountC",
                950.0, false, "TRANSFER",
                1000.0, 50.0, 5
        ));

        if (!detector.detectHighValueForwarding(outsideWindow)
                .isEmpty()) {
            throw new AssertionError(
                    "TEST 4 FAILED: Transfer outside window was detected"
            );
        }

        System.out.println("TEST 4 PASSED: Outside-window forwarding ignored");

        
        // TEST 5: Multiple incoming transfers
        List<Transaction> multipleIncoming = new ArrayList<>();

        multipleIncoming.add(new Transaction(
                "MI001", "AccountA", "AccountB",
                200.0, false, "TRANSFER",
                1000.0, 800.0, 1
        ));

        multipleIncoming.add(new Transaction(
                "MI002", "AccountD", "AccountB",
                1000.0, false, "TRANSFER",
                2000.0, 1000.0, 2
        ));

        multipleIncoming.add(new Transaction(
                "MI003", "AccountB", "AccountC",
                950.0, false, "TRANSFER",
                1200.0, 250.0, 3
        ));

        Set<String> multipleResults =
                detector.detectHighValueForwarding(multipleIncoming);

        if (!multipleResults.contains("AccountB")) {
            throw new AssertionError(
                    "TEST 5 FAILED: Multiple incoming transfers not handled"
            );
        }

        System.out.println(
                "TEST 5 PASSED: Multiple incoming transfers handled"
        );







        System.out.println("All TESTS PASSED");
    }
}