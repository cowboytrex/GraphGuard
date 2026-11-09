
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class HoldoutFraudEvaluation {

    private static final String FILE_PATH =
            "data/transactions.csv";

    private static final int HOLDOUT_START = 501;

    public static void main(String[] args) throws IOException {

        FraudDetectionService detector =
                new FraudDetectionService();

        long tp = 0;
        long fp = 0;
        long tn = 0;
        long fn = 0;
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

                if (step < HOLDOUT_START) {
                    continue;
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

                boolean predicted =
                        "HIGH RISK".equals(
                                detector.evaluateTransaction(transaction)
                        );

                if (predicted && isFraud) {
                    tp++;
                } else if (predicted) {
                    fp++;
                } else if (isFraud) {
                    fn++;
                } else {
                    tn++;
                }

                processed++;
            }
        }

        double precision = divide(tp, tp + fp);
        double recall = divide(tp, tp + fn);
        double f1 = divide(
                2 * precision * recall,
                precision + recall
        );
        double falsePositiveRate = divide(fp, fp + tn);

        System.out.println("=== GraphGuard Holdout Evaluation ===");
        System.out.println("Time steps: 501 onward");
        System.out.println("Transactions evaluated: " + processed);

        System.out.println("True Positives: " + tp);
        System.out.println("False Positives: " + fp);
        System.out.println("True Negatives: " + tn);
        System.out.println("False Negatives: " + fn);

        System.out.printf("Precision: %.2f%%%n", precision * 100);
        System.out.printf("Recall: %.2f%%%n", recall * 100);
        System.out.printf("F1 Score: %.4f%n", f1);
        System.out.printf(
                "False Positive Rate: %.4f%%%n",
                falsePositiveRate * 100
        );
    }

    private static double divide(
            double numerator,
            double denominator) {

        return denominator == 0 ? 0 : numerator / denominator;
    }
}
