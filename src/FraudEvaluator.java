
import java.util.List;

public class FraudEvaluator {

    public void evaluate(
            List<Transaction> transactions,
            FraudDetectionService detector) {

        int truePositive = 0;
        int falsePositive = 0;
        int trueNegative = 0;
        int falseNegative = 0;
        int printedFalsePositives = 0;
        int printedFalseNegatives = 0;

        for (Transaction transaction : transactions) {

            boolean actualFraud = transaction.isFraud();

            boolean predictedFraud =
                    detector.evaluateTransaction(transaction)
                            .equals("HIGH RISK");

            if (predictedFraud && actualFraud) {
                truePositive++;
           } else if (predictedFraud && !actualFraud) {
                falsePositive++;

                if (printedFalsePositives < 5) {
                    System.out.println(
                        "FALSE POSITIVE | Type: " + transaction.getType()
                        + " | Amount: " + transaction.getAmount()
                        + " | Old Balance: " + transaction.getOldBalance()
                        + " | New Balance: " + transaction.getNewBalance()
                        + " | Score: " + detector.calculateRiskScore(transaction)
                    );
                    printedFalsePositives++;
                }
            } else if (!predictedFraud && actualFraud) {
                falseNegative++;

                if (printedFalseNegatives < 5) {
                    System.out.println(
                        "FALSE NEGATIVE | Type: " + transaction.getType()
                        + " | Amount: " + transaction.getAmount()
                        + " | Old Balance: " + transaction.getOldBalance()
                        + " | New Balance: " + transaction.getNewBalance()
                        + " | Score: " + detector.calculateRiskScore(transaction)
                    );
                    printedFalseNegatives++;
                }
            } else {
                trueNegative++;
            }
        }

        System.out.println("=== Fraud Detection Evaluation ===");
        System.out.println("True Positives: " + truePositive);
        System.out.println("False Positives: " + falsePositive);
        System.out.println("True Negatives: " + trueNegative);
        System.out.println("False Negatives: " + falseNegative);

        
        int total = truePositive + falsePositive
                + trueNegative + falseNegative;

        double accuracy = total == 0 ? 0 :
                (double) (truePositive + trueNegative) / total;

        double precision = (truePositive + falsePositive) == 0 ? 0 :
                (double) truePositive / (truePositive + falsePositive);

        double recall = (truePositive + falseNegative) == 0 ? 0 :
                (double) truePositive / (truePositive + falseNegative);

        double f1Score = (precision + recall) == 0 ? 0 :
                2 * (precision * recall) / (precision + recall);

        double falsePositiveRate = (falsePositive + trueNegative) == 0 ? 0 :
                (double) falsePositive / (falsePositive + trueNegative);

        System.out.printf("Accuracy: %.2f%%%n", accuracy * 100);
        System.out.printf("Precision: %.2f%%%n", precision * 100);
        System.out.printf("Recall: %.2f%%%n", recall * 100);
        System.out.printf("F1 Score: %.2f%n", f1Score);
        System.out.printf("False Positive Rate: %.2f%%%n",
                falsePositiveRate * 100);

    }
}
