
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TimeAwareMultiHopEvaluation {

    private static final String FILE_PATH =
            "data/transactions.csv";

    private static final int MAX_STEP = 500;

    public static void main(String[] args) throws IOException {

        TransactionGraph graph = new TransactionGraph();

        TimeAwareMultiHopDetector detector =
                new TimeAwareMultiHopDetector(graph);

        long tp = 0;
        long fp = 0;
        long tn = 0;
        long fn = 0;

        long transfersProcessed = 0;
        long patternsFound = 0;

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

                if (!"TRANSFER".equals(type)) {
                    continue;
                }

                double amount = Double.parseDouble(c[2]);
                double oldBalance = Double.parseDouble(c[4]);
                double newBalance = Double.parseDouble(c[5]);
                boolean isFraud = Integer.parseInt(c[9]) == 1;

                Transaction transaction = new Transaction(
                        "TX-" + transfersProcessed,
                        c[3],
                        c[6],
                        amount,
                        isFraud,
                        type,
                        oldBalance,
                        newBalance,
                        step
                );

                // Detect using only previously observed transfers.
                boolean predicted =
                        detector.hasMultiHopPattern(transaction);

                if (predicted) {
                    patternsFound++;
                }

                if (predicted && isFraud) {
                    tp++;
                } else if (predicted) {
                    fp++;
                } else if (isFraud) {
                    fn++;
                } else {
                    tn++;
                }

                // Add current transaction only after prediction.
                graph.addTransaction(transaction);

                transfersProcessed++;
            }
        }

        double precision = divide(tp, tp + fp);
        double recall = divide(tp, tp + fn);
        double f1 = divide(
                2 * precision * recall,
                precision + recall
        );

        System.out.println("=== Time-Aware Multi-Hop Evaluation ===");
        System.out.println(
                "Transfers processed: " + transfersProcessed
        );
        System.out.println("Patterns found: " + patternsFound);

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
    }

    private static double divide(double numerator, double denominator) {
        return denominator == 0 ? 0 : numerator / denominator;
    }
}
