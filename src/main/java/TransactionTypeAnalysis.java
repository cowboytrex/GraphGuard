
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TransactionTypeAnalysis {

    public static void main(String[] args) throws IOException {

        Map<String, Long> totals = new HashMap<>();
        Map<String, Long> frauds = new HashMap<>();

        try (BufferedReader reader =
                new BufferedReader(new FileReader("data/transactions.csv"))) {

            reader.readLine(); // Skip CSV header

            String line;

            while ((line = reader.readLine()) != null) {

                String[] columns = line.split(",", -1);

                if (columns.length != 11) {
                    continue;
                }

                try {
                    int step = Integer.parseInt(columns[0]);

                    if (step > 500) {
                        break;
                    }

                    if (step < 1) {
                        continue;
                    }

                    String type = columns[1];
                    int isFraud = Integer.parseInt(columns[9]);

                    if (isFraud != 0 && isFraud != 1) {
                        continue;
                    }

                    totals.merge(type, 1L, Long::sum);
                    frauds.merge(type, (long) isFraud, Long::sum);

                } catch (NumberFormatException e) {
                    // Skip malformed rows
                }
            }
        }

        System.out.printf(
                "%-12s %12s %12s %12s%n",
                "Type", "Total", "Fraud", "Fraud Rate"
        );

        for (String type : totals.keySet()) {

            long total = totals.get(type);
            long fraud = frauds.getOrDefault(type, 0L);

            double rate = (double) fraud / total * 100;

            System.out.printf(
                    "%-12s %,12d %,12d %11.4f%%%n",
                    type, total, fraud, rate
            );
        }
    }
}
