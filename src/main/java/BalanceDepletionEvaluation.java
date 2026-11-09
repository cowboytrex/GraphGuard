
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class BalanceDepletionEvaluation {

    private static final String FILE_PATH =
            "data/transactions.csv";

    private static final int MAX_STEP = 500;

    private static class Metrics {
        long tp, fp, tn, fn;

        void record(boolean predicted, boolean actual) {
            if (predicted && actual) {
                tp++;
            } else if (predicted) {
                fp++;
            } else if (actual) {
                fn++;
            } else {
                tn++;
            }
        }

        void print(String name) {
            double precision = divide(tp, tp + fp);
            double recall = divide(tp, tp + fn);
            double f1 = divide(
                    2 * precision * recall,
                    precision + recall
            );

            System.out.println("\n=== " + name + " ===");
            System.out.println("True Positives: " + tp);
            System.out.println("False Positives: " + fp);
            System.out.println("True Negatives: " + tn);
            System.out.println("False Negatives: " + fn);

            System.out.printf(
                    "Precision: %.2f%%%n", precision * 100
            );
            System.out.printf(
                    "Recall: %.2f%%%n", recall * 100
            );
            System.out.printf(
                    "F1 Score: %.4f%n", f1
            );
            System.out.printf(
                    "False Positive Rate: %.4f%%%n",
                    divide(fp, fp + tn) * 100
            );
        }

        private double divide(double a, double b) {
            return b == 0 ? 0 : a / b;
        }
    }

    public static void main(String[] args) throws IOException {

        FraudDetectionService original =
                new FraudDetectionService();

        BalanceDepletionDetector newDetector =
                new BalanceDepletionDetector();

        Metrics originalMetrics = new Metrics();
        Metrics newMetrics = new Metrics();

        long processed = 0;
        long skipped = 0;

        try (BufferedReader reader =
                new BufferedReader(new FileReader(FILE_PATH))) {

            reader.readLine();

            String line;

            while ((line = reader.readLine()) != null) {

                String[] c = line.split(",", -1);

                if (c.length != 11) {
                    skipped++;
                    continue;
                }

                try {
                    int step = Integer.parseInt(c[0]);

                    if (step > MAX_STEP) {
                        break;
                    }

                    String type = c[1];

                    // Evaluate only transaction types
                    // containing labeled fraud.
                    if (!"TRANSFER".equals(type)
                            && !"CASH_OUT".equals(type)) {
                        continue;
                    }

                    double amount = Double.parseDouble(c[2]);
                    double oldBalance = Double.parseDouble(c[4]);
                    double newBalance = Double.parseDouble(c[5]);
                    int fraud = Integer.parseInt(c[9]);

                    if (!Double.isFinite(amount)
                            || !Double.isFinite(oldBalance)
                            || !Double.isFinite(newBalance)
                            || amount < 0
                            || (fraud != 0 && fraud != 1)) {
                        skipped++;
                        continue;
                    }

                    Transaction transaction = new Transaction(
                            "TX-" + processed,
                            c[3],
                            c[6],
                            amount,
                            fraud == 1,
                            type,
                            oldBalance,
                            newBalance,
                            step
                    );

                    boolean actualFraud = transaction.isFraud();

                    boolean originalPrediction =
                            original.evaluateTransaction(transaction)
                                    .equals("HIGH RISK");

                    boolean newPrediction =
                            newDetector.isNearFullDepletion(transaction);

                    originalMetrics.record(
                            originalPrediction, actualFraud
                    );

                    newMetrics.record(
                            newPrediction, actualFraud
                    );

                    processed++;

                } catch (NumberFormatException e) {
                    skipped++;
                }
            }
        }

        System.out.println("=== Evaluation Summary ===");
        System.out.println("Transactions evaluated: " + processed);
        System.out.println("Rows skipped: " + skipped);

        originalMetrics.print("Original Fraud Detection");
        newMetrics.print("Balance Depletion Detection");
    }
}
