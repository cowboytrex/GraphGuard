
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class FraudPatternAnalysis {

    private static final String FILE_PATH =
            "data/transactions.csv";

    private static final int MAX_STEP = 500;
    private static final double DRAIN_THRESHOLD = 0.95;

    private static class Stats {
        long total = 0;
        long fraud = 0;

        double legitimateAmountSum = 0;
        double fraudAmountSum = 0;

        long legitimateCount = 0;
        long fraudCount = 0;

        long legitimateDrain = 0;
        long fraudDrain = 0;

        long legitimateZeroBalance = 0;
        long fraudZeroBalance = 0;
    }

    public static void main(String[] args) throws IOException {

        Map<String, Stats> byType = new LinkedHashMap<>();

        try (BufferedReader reader =
                new BufferedReader(new FileReader(FILE_PATH))) {

            reader.readLine();

            String line;

            while ((line = reader.readLine()) != null) {

                String[] columns = line.split(",", -1);

                if (columns.length != 11) {
                    continue;
                }

                int step;
                double amount;
                double oldBalance;
                double newBalance;
                int fraud;

                try {
                    step = Integer.parseInt(columns[0]);
                    amount = Double.parseDouble(columns[2]);
                    oldBalance = Double.parseDouble(columns[4]);
                    newBalance = Double.parseDouble(columns[5]);
                    fraud = Integer.parseInt(columns[9]);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (step > MAX_STEP) {
                    break;
                }

                if (!Double.isFinite(amount)
                        || !Double.isFinite(oldBalance)
                        || !Double.isFinite(newBalance)
                        || amount < 0
                        || (fraud != 0 && fraud != 1)) {
                    continue;
                }

                String type = columns[1];

                Stats stats = byType.computeIfAbsent(
                        type,
                        key -> new Stats()
                );

                stats.total++;

                boolean drainsAccount =
                        oldBalance > 0
                        && amount >= oldBalance * DRAIN_THRESHOLD;

                boolean zeroBalance =
                        oldBalance > 0 && newBalance == 0;

                if (fraud == 1) {
                    stats.fraud++;
                    stats.fraudCount++;
                    stats.fraudAmountSum += amount;

                    if (drainsAccount) {
                        stats.fraudDrain++;
                    }

                    if (zeroBalance) {
                        stats.fraudZeroBalance++;
                    }

                } else {
                    stats.legitimateCount++;
                    stats.legitimateAmountSum += amount;

                    if (drainsAccount) {
                        stats.legitimateDrain++;
                    }

                    if (zeroBalance) {
                        stats.legitimateZeroBalance++;
                    }
                }
            }
        }

        System.out.println("=== Fraud Pattern Analysis ===");

        for (Map.Entry<String, Stats> entry : byType.entrySet()) {

            String type = entry.getKey();
            Stats s = entry.getValue();

            System.out.println();
            System.out.println("Type: " + type);
            System.out.println("Total: " + s.total);
            System.out.println("Fraud: " + s.fraud);

            System.out.printf(
                    "Fraud rate: %.4f%%%n",
                    percentage(s.fraud, s.total)
            );

            System.out.printf(
                    "Average legitimate amount: %.2f%n",
                    average(s.legitimateAmountSum, s.legitimateCount)
            );

            System.out.printf(
                    "Average fraud amount: %.2f%n",
                    average(s.fraudAmountSum, s.fraudCount)
            );

            System.out.printf(
                    "Legitimate drain rate: %.2f%%%n",
                    percentage(s.legitimateDrain, s.legitimateCount)
            );

            System.out.printf(
                    "Fraud drain rate: %.2f%%%n",
                    percentage(s.fraudDrain, s.fraudCount)
            );

            System.out.printf(
                    "Legitimate zero-balance rate: %.2f%%%n",
                    percentage(s.legitimateZeroBalance, s.legitimateCount)
            );

            System.out.printf(
                    "Fraud zero-balance rate: %.2f%%%n",
                    percentage(s.fraudZeroBalance, s.fraudCount)
            );
        }
    }

    private static double percentage(long part, long total) {
        return total == 0 ? 0 : 100.0 * part / total;
    }

    private static double average(double sum, long count) {
        return count == 0 ? 0 : sum / count;
    }
}
