
public class Transaction {

    private String transactionId;
    private String sender;
    private String receiver;
    private double amount;

    public Transaction(String transactionId,
                       String sender,
                       String receiver,
                       double amount) {

        this.transactionId = transactionId;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
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
}
