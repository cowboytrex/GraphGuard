
public class BalanceDepletionDetectorTest {

    private static final BalanceDepletionDetector detector =
            new BalanceDepletionDetector();

    private static Transaction transaction(
            String type,
            double amount,
            double oldBalance,
            double newBalance) {

        return new Transaction(
                "TEST-001",
                "Account-A",
                "Account-B",
                amount,
                false,
                type,
                oldBalance,
                newBalance,
                1
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

        check(
                "Full balance depletion",
                true,
                detector.isNearFullDepletion(
                        transaction("TRANSFER", 10000, 10000, 0))
        );

        check(
                "97% balance depletion",
                true,
                detector.isNearFullDepletion(
                        transaction("TRANSFER", 9700, 10000, 300))
        );

        check(
                "50% balance depletion",
                false,
                detector.isNearFullDepletion(
                        transaction("TRANSFER", 5000, 10000, 5000))
        );

        check(
                "Unsupported transaction type",
                false,
                detector.isNearFullDepletion(
                        transaction("PAYMENT", 10000, 10000, 0))
        );

        check(
                "Inconsistent ending balance",
                false,
                detector.isNearFullDepletion(
                        transaction("CASH_OUT", 9700, 10000, 1000))
        );

        check(
                "Zero starting balance",
                false,
                detector.isNearFullDepletion(
                        transaction("TRANSFER", 1000, 0, 0))
        );

        check(
                "Cash-out depletion",
                true,
                detector.isNearFullDepletion(
                        transaction("CASH_OUT", 9800, 10000, 200))
        );

        System.out.println("All balance depletion tests passed.");
    }
}

