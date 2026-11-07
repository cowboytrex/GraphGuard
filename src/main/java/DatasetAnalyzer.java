
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class DatasetAnalyzer {

    public static void main(String[] args) throws IOException {

        String filePath = "data/transactions.csv";

        Map<Integer, long[]> statistics = new TreeMap<>();

        long totalTransactions = 0;
        long totalFraud = 0;
        int minStep = Integer.MAX_VALUE;
        int maxStep = Integer.MIN_VALUE;

        try (BufferedReader reader =
                new BufferedReader(new FileReader(filePath))) {

            reader.readLine(); // Skip header

            String line;

            while ((line = reader.readLine()) != null) {

                String[] columns = line.split(",", -1);

                if (columns.length != 11) {
                    continue;
                }

                try {
                    int step = Integer.parseInt(columns[0]);
                    int fraud = Integer.parseInt(columns[9]);

                    if (fraud != 0 && fraud != 1) {
                        continue;
                    }

                    int period = (step - 1) / 100;

                    long[] counts = statistics.computeIfAbsent(
                            period, key -> new long[2]
                    );

                    counts[0]++;
                    counts[1] += fraud;

                    totalTransactions++;
                    totalFraud += fraud;

                    minStep = Math.min(minStep, step);
                    maxStep = Math.max(maxStep, step);

                } catch (NumberFormatException e) {
                    // Skip malformed rows
                }
            }
        }

        System.out.println("=== PaySim Dataset Analysis ===");
        System.out.println("Total transactions: " + totalTransactions);
        System.out.println("Total fraud cases: " + totalFraud);
        System.out.println("Minimum step: " + minStep);
        System.out.println("Maximum step: " + maxStep);

        System.out.println();
        System.out.println("=== Transactions by Time Period ===");

        for (Map.Entry<Integer, long[]> entry : statistics.entrySet()) {

            int period = entry.getKey();
            long[] counts = entry.getValue();

            int startStep = period * 100 + 1;
            int endStep = (period + 1) * 100;

            System.out.printf(
                    "Steps %d-%d | Transactions: %d | Fraud: %d%n",
                    startStep,
                    endStep,
                    counts[0],
                    counts[1]
            );
        }
    }
}
