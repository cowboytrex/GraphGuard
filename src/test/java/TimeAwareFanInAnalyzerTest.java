
public class TimeAwareFanInAnalyzerTest {

    private static int passed = 0;

    private static void check(
            String testName,
            boolean actual,
            boolean expected
    ) {
        if (actual != expected) {
            throw new AssertionError(
                    testName + " FAILED. Expected: "
                    + expected + ", Actual: " + actual
            );
        }

        passed++;
        System.out.println("PASS: " + testName);
    }

    public static void main(String[] args) {

        // Test 1: Three distinct senders
        TimeAwareFanInAnalyzer analyzer1 =
                new TimeAwareFanInAnalyzer();

        analyzer1.isSuspicious(10, "A", "X");
        analyzer1.isSuspicious(15, "B", "X");

        check(
                "Three distinct senders",
                analyzer1.isSuspicious(20, "C", "X"),
                true
        );

        // Test 2: Repeated sender
        TimeAwareFanInAnalyzer analyzer2 =
                new TimeAwareFanInAnalyzer();

        analyzer2.isSuspicious(10, "A", "X");
        analyzer2.isSuspicious(11, "A", "X");

        check(
                "Repeated sender",
                analyzer2.isSuspicious(12, "A", "X"),
                false
        );

        // Test 3: Expired transactions
        TimeAwareFanInAnalyzer analyzer3 =
                new TimeAwareFanInAnalyzer();

        analyzer3.isSuspicious(10, "A", "X");
        analyzer3.isSuspicious(15, "B", "X");

        check(
                "Expired transactions",
                analyzer3.isSuspicious(40, "C", "X"),
                false
        );

        // Test 4: Independent receivers
        TimeAwareFanInAnalyzer analyzer4 =
                new TimeAwareFanInAnalyzer();

        analyzer4.isSuspicious(10, "A", "X");
        analyzer4.isSuspicious(11, "B", "X");

        check(
                "Independent receivers",
                analyzer4.isSuspicious(12, "C", "Y"),
                false
        );

        // Test 5: Exact 24-step boundary
        TimeAwareFanInAnalyzer analyzer5 =
                new TimeAwareFanInAnalyzer();

        analyzer5.isSuspicious(1, "A", "X");
        analyzer5.isSuspicious(2, "B", "X");

        check(
                "Window boundary",
                analyzer5.isSuspicious(25, "C", "X"),
                false
        );

        System.out.println();
        System.out.println("Passed: " + passed + "/5");
    }
}
