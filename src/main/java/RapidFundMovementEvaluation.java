
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class RapidFundMovementEvaluation {

    private static final String FILE_PATH =
            "data/transactions.csv";

    private static final int MAX_STEP = 500;

    public static void main(String[] args) throws IOException {

        RapidFundMovementDetector detector =
                new RapidFundMovementDetector();

        long truePositive = 0;
        long falsePositive = 0;
        long trueNegative = 0;
        long falseNegative = 0;

        long transferCount = 0;
        long cashOutCount = 0;

        int lastStep = -1;

        try (BufferedReader reader =
                new BufferedReader(new FileReader(FILE_PATH))) {

            reader.readLine(); // Skip header

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

                    if (step < lastStep) {
                        throw new IllegalStateException(
                                "Transactions are not in chronological order"
                        );
                    }

                    lastStep = step;

                    String type = columns[1];

                    if (!type.equals("TRANSFER")
                            && !type.equals("CASH_OUT")) {
                        continue;
                    }

                    double amount =
                            Double.parseDouble(columns[2]);

                    String sender = columns[3];
                    String receiver = columns[6];

                    int fraud = Integer.parseInt(columns[9]);

                    if (fraud != 0 && fraud != 1) {
                        continue;
                    }

                    Transaction transaction = new Transaction(
                            "TX-" + step + "-" + transferCount
                                    + "-" + cashOutCount,
                            sender,
                            receiver,
                            amount,
                            fraud == 1,
                            type,
                            Double.parseDouble(columns[4]),
                            Double.parseDouble(columns[5]),
                            step
                    );

                    boolean predicted =
                            detector.analyze(transaction);

                    if (type.equals("TRANSFER")) {
                        transferCount++;
                        continue;
                    }

                    cashOutCount++;

                    boolean actual = transaction.isFraud();

                    if (predicted && actual) {
                        truePositive++;
                    } else if (predicted) {
                        falsePositive++;
                    } else if (actual) {
                        falseNegative++;
                    } else {
                        trueNegative++;
                    }

                } catch (NumberFormatException e) {
                    // Skip malformed numeric rows
                }
            }
        }

        double precision = (truePositive + falsePositive) == 0
                ? 0
                : (double) truePositive
                        / (truePositive + falsePositive);

        double recall = (truePositive + falseNegative) == 0
                ? 0
                : (double) truePositive
                        / (truePositive + falseNegative);

        double f1 = (precision + recall) == 0
                ? 0
                : 2 * precision * recall / (precision + recall);

        System.out.println(
                "=== Rapid Fund Movement Evaluation ==="
        );

        System.out.println("Transfers processed: " + transferCount);
        System.out.println("Cash-outs evaluated: " + cashOutCount);

        System.out.println("True Positives: " + truePositive);
        System.out.println("False Positives: " + falsePositive);
        System.out.println("True Negatives: " + trueNegative);
        System.out.println("False Negatives: " + falseNegative);

        System.out.printf("Precision: %.2f%%%n", precision * 100);
        System.out.printf("Recall: %.2f%%%n", recall * 100);
        System.out.printf("F1 Score: %.4f%n", f1);
    }
}

