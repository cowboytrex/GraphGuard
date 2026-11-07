
public class Transaction {

    private String transactionId;
    private String sender;
    private String receiver;
    private double amount;
    private boolean isFraud;

    private String type;
    private double oldBalance;
    private double newBalance;
    private int step;

    public Transaction(
            String transactionId,
            String sender,
            String receiver,
            double amount,
            boolean isFraud,
            String type,
            double oldBalance,
            double newBalance,
            int step) {

        this.transactionId = transactionId;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.isFraud = isFraud;

        this.type = type;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.step = step;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isFraud() {
        return isFraud;
    }

    public String getType() {
        return type;
    }

    public double getOldBalance() {
        return oldBalance;
    }

    public double getNewBalance() {
        return newBalance;
    }

    public int getStep() {
        return step;
    }
}
