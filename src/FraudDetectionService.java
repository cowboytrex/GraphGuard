
public class FraudDetectionService {

    public int calculateRiskScore(Transaction transaction) {

        int riskScore = 0;

        double amount = transaction.getAmount();
        String type = transaction.getType();
        double oldBalance = transaction.getOldBalance();
        double newBalance = transaction.getNewBalance();

        // Rule 1: Transaction category
        boolean highRiskType =
                type.equals("TRANSFER") || type.equals("CASH_OUT");

        if (highRiskType) {
            riskScore += 15;
        }

        // Rule 2: Large transaction
        if (amount > 10000) {
            riskScore += 10;
        }

        // Rule 3: Very large transaction
        if (amount > 100000) {
            riskScore += 5;
        }

        // Rule 4: Account-draining transaction
        boolean drainsAccount =
                oldBalance > 0
                && Math.abs(oldBalance - amount) < 0.01
                && Math.abs(newBalance) < 0.01;

        if (highRiskType && drainsAccount) {
            riskScore += 50;
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
