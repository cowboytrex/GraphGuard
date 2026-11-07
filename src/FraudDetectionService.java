
public class FraudDetectionService {

    public int calculateRiskScore(Transaction transaction) {

        double amount = transaction.getAmount();
        int riskScore = 0;

        if (amount > 10000) {
            riskScore += 50;
        } else if (amount >= 5000) {
            riskScore += 25;
        }

        return riskScore;
    }

    public String evaluateTransaction(Transaction transaction) {

        int riskScore = calculateRiskScore(transaction);

        if (riskScore >= 50) {
            return "HIGH RISK";
        } else if (riskScore >= 25) {
            return "MEDIUM RISK";
        } else {
            return "LOW RISK";
        }
    }
}
