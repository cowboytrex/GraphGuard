
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MissedFraudDiagnostics {

    private static final String FILE_PATH =
            "data/transactions.csv";

    private static final int MAX_STEP = 500;

    public static void main(String[] args) throws IOException {

        FraudDetectionService detector =
                new FraudDetectionService();

        long missed = 0;
        long zeroOldBalance = 0;
        long amountMismatch = 0;
        long nonZeroNewBalance = 0;
        long other = 0;

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

                int fraud = Integer.parseInt(c[9]);

                if (fraud != 1) {
                    continue;
                }

                double amount = Double.parseDouble(c[2]);
                double oldBalance = Double.parseDouble(c[4]);
                double newBalance = Double.parseDouble(c[5]);

                Transaction transaction = new Transaction(
                        "TX-" + step,
                        c[3],
                        c[6],
                        amount,
                        true,
                        type,
                        oldBalance,
                        newBalance,
                        step
                );

                if ("HIGH RISK".equals(
                        detector.evaluateTransaction(transaction))) {
                    continue;
                }

                missed++;

                boolean zeroOld = oldBalance <= 0;
                boolean mismatch =
                        Math.abs(oldBalance - amount) >= 0.01;
                boolean nonZeroNew =
                        Math.abs(newBalance) >= 0.01;

                if (zeroOld) zeroOldBalance++;
                if (mismatch) amountMismatch++;
                if (nonZeroNew) nonZeroNewBalance++;

                if (!zeroOld && !mismatch && !nonZeroNew) {
                    other++;
                }

                if (missed <= 10) {
                    System.out.printf(
                            "Step %d | Type: %s"
                            + " | Amount: %.2f"
                            + " | Old: %.2f"
                            + " | New: %.2f"
                            + " | Score: %d%n",
                            step,
                            type,
                            amount,
                            oldBalance,
                            newBalance,
                            detector.calculateRiskScore(transaction)
                    );
                }
            }
        }

        System.out.println();
        System.out.println("=== Missed Fraud Diagnostics ===");
        System.out.println("Total missed: " + missed);
        System.out.println("Zero/negative old balance: " + zeroOldBalance);
        System.out.println("Amount mismatch: " + amountMismatch);
        System.out.println("Non-zero ending balance: " + nonZeroNewBalance);
        System.out.println("Other: " + other);
    }
}
