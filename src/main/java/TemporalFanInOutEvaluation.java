
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TemporalFanInOutEvaluation {

    private static final String FILE_PATH =
            "data/transactions.csv";

    private static final int MAX_STEP = 500;

    private static final int[] THRESHOLDS = {2, 3, 5};

    public static void main(String[] args) throws IOException {

        TransactionGraph graph = new TransactionGraph();

        TemporalFanInOutDetector detector =
                new TemporalFanInOutDetector(graph);

        long[] tp = new long[THRESHOLDS.length];
        long[] fp = new long[THRESHOLDS.length];
        long[] tn = new long[THRESHOLDS.length];
        long[] fn = new long[THRESHOLDS.length];

        long processed = 0;

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

                Transaction transaction = new Transaction(
                        "TX-" + processed,
                        c[3],
                        c[6],
                        amount,
                        isFraud,
                        type,
                        oldBalance,
                        newBalance,
                        step
                );

                int fanIn =
                        detector.countRecentDistinctSenders(
                                transaction.getReceiver(), step
                        );

                int fanOut =
                        detector.countRecentDistinctReceivers(
                                transaction.getSender(), step
                        );

                for (int i = 0; i < THRESHOLDS.length; i++) {

                    boolean predicted =
                            fanIn >= THRESHOLDS[i]
                            || fanOut >= THRESHOLDS[i];

                    if (predicted && isFraud) {
                        tp[i]++;
                    } else if (predicted) {
                        fp[i]++;
                    } else if (isFraud) {
                        fn[i]++;
                    } else {
                        tn[i]++;
                    }
                }

                // Add only after evaluating current transaction.
                graph.addTransaction(transaction);
                processed++;
            }
        }

        System.out.println(
                "=== Temporal Fan-In / Fan-Out Evaluation ==="
        );
        System.out.println("Transactions evaluated: " + processed);

        for (int i = 0; i < THRESHOLDS.length; i++) {

            double precision = divide(tp[i], tp[i] + fp[i]);
            double recall = divide(tp[i], tp[i] + fn[i]);
            double f1 = divide(
                    2 * precision * recall,
                    precision + recall
            );

            System.out.println(
                    "\nThreshold: " + THRESHOLDS[i]
            );

            System.out.println("TP: " + tp[i]);
            System.out.println("FP: " + fp[i]);
            System.out.println("TN: " + tn[i]);
            System.out.println("FN: " + fn[i]);

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
    }

    private static double divide(
            double numerator,
            double denominator) {

        return denominator == 0 ? 0 : numerator / denominator;
    }
}
