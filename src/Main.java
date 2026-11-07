public class Main {

    public static void main(String[] args) {

        Transaction tx1 = new Transaction(
            "TX001", "Alice", "Bob", 15000
        );

        FraudDetectionService detector = new FraudDetectionService();

        String result = detector.evaluateTransaction(tx1);

        System.out.println("Transaction: " + tx1.getTransactionId());
        System.out.println("Risk Level: " + result);
    }
}
