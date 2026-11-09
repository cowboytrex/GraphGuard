
public class BalanceDepletionDetector {

    private static final double DRAIN_THRESHOLD = 0.95;
    private static final double BALANCE_TOLERANCE = 0.01;

    public boolean isHighRiskType(Transaction transaction) {

        String type = transaction.getType();

        return "TRANSFER".equals(type)
                || "CASH_OUT".equals(type);
    }

    public double calculateDepletionRatio(Transaction transaction) {

        double oldBalance = transaction.getOldBalance();
        double amount = transaction.getAmount();

        if (!Double.isFinite(oldBalance)
                || !Double.isFinite(amount)
                || oldBalance <= 0
                || amount < 0) {
            return 0;
        }

        return amount / oldBalance;
    }

    public boolean isNearFullDepletion(Transaction transaction) {

        if (!isHighRiskType(transaction)) {
            return false;
        }

        double oldBalance = transaction.getOldBalance();
        double newBalance = transaction.getNewBalance();
        double amount = transaction.getAmount();

        if (!Double.isFinite(newBalance)) {
            return false;
        }

        boolean nearlyAllTransferred =
                calculateDepletionRatio(transaction) >= DRAIN_THRESHOLD;

        boolean balanceConsistent =
                Math.abs((oldBalance - amount) - newBalance)
                        <= BALANCE_TOLERANCE;

        return nearlyAllTransferred && balanceConsistent;
    }
}
