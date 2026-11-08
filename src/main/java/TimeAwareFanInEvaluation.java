
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TimeAwareFanInEvaluation {

    private static final String FILE_PATH =
            "data/transactions.csv";

    private static final int MAX_STEP = 500;

    public static void main(String[] args) throws IOException {

        TimeAwareFanInAnalyzer analyzer =
                new TimeAwareFanInAnalyzer();

        long highTransactions = 0;
        long lowTransactions = 0;

        long highFraud = 0;
        long lowFraud = 0;

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

                    if (step > MAX_STEP) {
                        break;
                    }

                    if (step < 1) {
                        continue;
                    }
                    
                    String transactionType = columns[1];

                    if (!transactionType.equals("TRANSFER")) {
                        continue;
                    }

                    int fraud = Integer.parseInt(columns[9]);


                    if (fraud != 0 && fraud != 1) {
                        continue;
                    }

                    String sender = columns[3];
                    String receiver = columns[6];

                    boolean suspicious =
                            analyzer.isSuspicious(
                                    step,
                                    sender,
                                    receiver
                            );

                    if (suspicious) {
                        highTransactions++;
                        highFraud += fraud;
                    } else {
                        lowTransactions++;
                        lowFraud += fraud;
                    }

                } catch (NumberFormatException e) {
                    // Skip malformed numeric values
                }
            }
        }

        System.out.println(
                "=== Time-Aware Fan-In Evaluation ==="
        );

        printResults(
                "High fan-in",
                highTransactions,
                highFraud
        );

        printResults(
                "Low fan-in",
                lowTransactions,
                lowFraud
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
        System.out.printf(
                "Fraud rate: %.4f%%%n",
                fraudRate
        );
    }
}
