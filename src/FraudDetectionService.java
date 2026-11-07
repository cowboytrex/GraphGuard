public class FraudDetectionService {

    public String evaluateTransaction(Transaction transaction) {

        double amount = transaction.getAmount();

        if (amount > 10000) {
            return "HIGH RISK";
        } else if (amount >= 5000) {
            return "MEDIUM RISK";
        } else {
            return "LOW RISK";
        }
    }
}
