
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TenMillionAmountDiagnostics {

    private static final String FILE_PATH =
            "data/transactions.csv";

    private static final int MAX_STEP = 500;
    private static final double TARGET_AMOUNT = 10_000_000.0;
    private static final double TOLERANCE = 0.01;

    public static void main(String[] args) throws IOException {

        FraudDetectionService detector =
                new FraudDetectionService();

        long missedFraud = 0;
        long missedFraudAtTarget = 0;
        long legitimateAtTarget = 0;
        long legitimateTotal = 0;

        try (BufferedReader reader =
                new BufferedReader(new FileReader(FILE_PATH))) {

            reader.readLine();

            String line;

            while ((line = reader.readLine()) != null) {

                String[] c = line.split(",", -1);

                if (c.length != 11) {
                    continue;
                }

                int step = Integer.parseInt(c[0]);

                if (step > MAX_STEP) {
                    break;
                }

                String type = c[1];

                if (!"TRANSFER".equals(type)
                        && !"CASH_OUT".equals(type)) {
                    continue;
                }

                double amount = Double.parseDouble(c[2]);
                double oldBalance = Double.parseDouble(c[4]);
                double newBalance = Double.parseDouble(c[5]);
                boolean isFraud = Integer.parseInt(c[9]) == 1;

                if (!Double.isFinite(amount)
                        || !Double.isFinite(oldBalance)
                        || !Double.isFinite(newBalance)
                        || amount < 0) {
                    continue;
                }

                Transaction transaction = new Transaction(
                        "TX-" + step,
                        c[3],
                        c[6],
                        amount,
                        isFraud,
                        type,
                        oldBalance,
                        newBalance,
                        step
                );

                boolean predicted =
                        "HIGH RISK".equals(
                                detector.evaluateTransaction(transaction)
                        );

                boolean atTarget =
                        Math.abs(amount - TARGET_AMOUNT) < TOLERANCE;

                if (isFraud && !predicted) {
                    missedFraud++;

                    if (atTarget) {
                        missedFraudAtTarget++;
                    }
                }

                if (!isFraud) {
                    legitimateTotal++;

                    if (atTarget) {
                        legitimateAtTarget++;
                    }
                }
            }
        }

        System.out.println("=== $10M Amount Diagnostics ===");
        System.out.println("Missed fraud: " + missedFraud);
        System.out.println(
                "Missed fraud at $10M: " + missedFraudAtTarget
        );
        System.out.println(
                "Legitimate transactions at $10M: "
                        + legitimateAtTarget
        );
        System.out.println(
                "Total legitimate transactions: "
                        + legitimateTotal
        );
    }
}
