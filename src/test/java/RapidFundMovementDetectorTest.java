
public class RapidFundMovementDetectorTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {

        // Test 1: Rapid cash-out
        test("Rapid cash-out", 100, 101,
                "AccountB", 4900, true);

        // Test 2: Outside the 24-step window
        test("Expired transfer", 100, 130,
                "AccountB", 4900, false);

        // Test 3: Cash-out below 80%
        test("Small cash-out", 100, 101,
                "AccountB", 1000, false);

        // Test 4: Different sender account
        test("Unrelated account", 100, 101,
                "AccountC", 4900, false);

        // Test 5: Cash-out before transfer
        test("Earlier cash-out", 100, 99,
                "AccountB", 4900, false);

        // Test 6: Cash-out exceeds incoming transfer
        test("Excessive cash-out", 100, 101,
                "AccountB", 20000, false);

        // Test 7: Prevent duplicate cash-out matching
        testDuplicateCashOut();

        // Print test summary
        System.out.println();
        System.out.println("Tests passed: " + passed);
        System.out.println("Tests failed: " + failed);

        if (failed > 0) {
            throw new AssertionError("Some tests failed");
        }
    }

    private static void test(
            String name,
            int transferStep,
            int cashOutStep,
            String cashOutSender,
            double cashOutAmount,
            boolean expected) {

        RapidFundMovementDetector detector =
                new RapidFundMovementDetector();

        Transaction incoming = new Transaction(
                "TX001", "AccountA", "AccountB",
                5000.0, false, "TRANSFER",
                10000.0, 5000.0, transferStep
        );

        Transaction outgoing = new Transaction(
                "TX002", cashOutSender, "AccountD",
                cashOutAmount, false, "CASH_OUT",
                5000.0, 100.0, cashOutStep
        );

        detector.analyze(incoming);
        boolean actual = detector.analyze(outgoing);

        if (actual == expected) {
            passed++;
            System.out.println("PASS: " + name);
        } else {
            failed++;
            System.out.println(
                    "FAIL: " + name
                    + " | Expected: " + expected
                    + " | Actual: " + actual
            );
        }
    }

    private static void testDuplicateCashOut() {

        RapidFundMovementDetector detector =
                new RapidFundMovementDetector();

        Transaction incoming = new Transaction(
                "TX100", "A", "B", 5000.0,
                false, "TRANSFER", 10000, 5000, 100
        );

        Transaction firstCashOut = new Transaction(
                "TX101", "B", "C", 4900.0,
                false, "CASH_OUT", 5000, 100, 101
        );

        Transaction secondCashOut = new Transaction(
                "TX102", "B", "D", 4800.0,
                false, "CASH_OUT", 100, 0, 102
        );

        detector.analyze(incoming);

        boolean first = detector.analyze(firstCashOut);
        boolean second = detector.analyze(secondCashOut);

        if (first && !second) {
            passed++;
            System.out.println("PASS: Duplicate cash-out prevention");
        } else {
            failed++;
            System.out.println(
                    "FAIL: Duplicate cash-out prevention"
                    + " | First: " + first
                    + " | Second: " + second
            );
        }
    }
}
