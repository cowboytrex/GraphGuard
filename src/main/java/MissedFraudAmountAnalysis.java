
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MissedFraudAmountAnalysis {

    private static final String FILE_PATH =
            "data/transactions.csv";

    private static final int MAX_STEP = 500;

    public static void main(String[] args) throws IOException {

        FraudDetectionService detector =
                new FraudDetectionService();

        List<Double> detectedFraud = new ArrayList<>();
        List<Double> missedFraud = new ArrayList<>();
        List<Double> legitimate = new ArrayList<>();

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

                if (isFraud && predicted) {
                    detectedFraud.add(amount);
                } else if (isFraud) {
                    missedFraud.add(amount);
                } else {
                    legitimate.add(amount);
                }
            }
        }

        printStats("Detected Fraud", detectedFraud);
        printStats("Missed Fraud", missedFraud);
        printStats("Legitimate Transactions", legitimate);
    }

    private static void printStats(
            String label,
            List<Double> amounts) {

        Collections.sort(amounts);

        System.out.println();
        System.out.println("=== " + label + " ===");
        System.out.println("Count: " + amounts.size());

        if (amounts.isEmpty()) {
            return;
        }

        System.out.printf(
                "Median: %.2f%n",
                percentile(amounts, 0.50)
        );

        System.out.printf(
                "90th percentile: %.2f%n",
                percentile(amounts, 0.90)
        );

        System.out.printf(
                "99th percentile: %.2f%n",
                percentile(amounts, 0.99)
        );

        System.out.printf(
                "Maximum: %.2f%n",
                amounts.get(amounts.size() - 1)
        );
    }

    private static double percentile(
            List<Double> sorted,
            double p) {

        if (sorted.size() == 1) {
            return sorted.get(0);
        }

        double index = p * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);

        if (lower == upper) {
            return sorted.get(lower);
        }

        double weight = index - lower;

        return sorted.get(lower)
                + weight * (sorted.get(upper) - sorted.get(lower));
    }
}
