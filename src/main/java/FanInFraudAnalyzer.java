
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FanInFraudAnalyzer {

    private static final String FILE_PATH = "data/transactions.csv";
    private static final int MAX_STEP = 500;
    private static final int FAN_IN_THRESHOLD = 3;

    public static void main(String[] args) throws IOException {

        Map<String, Set<String>> receiverSenders =
                new HashMap<>();

        // Pass 1: Count distinct senders for each receiver
        try (BufferedReader reader =
                     new BufferedReader(new FileReader(FILE_PATH))) {

            reader.readLine(); // Skip CSV header

            String line;

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", -1);

                if (columns.length != 11) {
                    continue;
                }

                try {
                    int step = Integer.parseInt(columns[0]);

                    if (step < 1 || step > MAX_STEP) {
                        continue;
                    }

                    String sender = columns[2];
                    String receiver = columns[6];

                    receiverSenders
                            .computeIfAbsent(
                                    receiver,
                                    key -> new HashSet<>()
                            )
                            .add(sender);

                } catch (NumberFormatException e) {
                    // Skip malformed rows
                }
            }
        }

        long lowTransactions = 0;
        long highTransactions = 0;
        long lowFraud = 0;
        long highFraud = 0;

        // Pass 2: Compare fraud rates by fan-in group
        try (BufferedReader reader =
                     new BufferedReader(new FileReader(FILE_PATH))) {

            reader.readLine();

            String line;

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", -1);

                if (columns.length != 11) {
                    continue;
                }

                try {
                    int step = Integer.parseInt(columns[0]);

                    if (step < 1 || step > MAX_STEP) {
                        continue;
                    }

                    int fraud = Integer.parseInt(columns[9]);

                    if (fraud != 0 && fraud != 1) {
                        continue;
                    }

                    String receiver = columns[6];

                    int distinctSenders = receiverSenders
                            .get(receiver)
                            .size();

                    if (distinctSenders >= FAN_IN_THRESHOLD) {
                        highTransactions++;
                        highFraud += fraud;
                    } else {
                        lowTransactions++;
                        lowFraud += fraud;
                    }

                } catch (NumberFormatException e) {
                    // Skip malformed rows
                }
            }
        }

        System.out.println("=== GraphGuard Fan-In Fraud Analysis ===");

        printResults(
                "Low fan-in",
                lowTransactions,
                lowFraud
        );

        printResults(
                "High fan-in",
                highTransactions,
                highFraud
        );
    }

    private static void printResults(
            String group,
            long transactions,
            long fraud
    ) {
        double fraudRate = transactions == 0
                ? 0
                : (double) fraud / transactions * 100;

        System.out.println();
        System.out.println(group);
        System.out.println("Transactions: " + transactions);
        System.out.println("Fraud cases: " + fraud);
        System.out.printf("Fraud rate: %.4f%%%n", fraudRate);
    }

}
