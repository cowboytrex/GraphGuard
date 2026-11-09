import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

public class InvestigationReport {

    private final Transaction transaction;
    private final FraudDetectionService fraudService;
    private final TransactionGraph graph;

    public InvestigationReport(
            Transaction transaction,
            FraudDetectionService fraudService,
            TransactionGraph graph) {

        this.transaction = transaction;
        this.fraudService = fraudService;
        this.graph = graph;
    }

    public List<String> getRiskExplanations() {

        List<String> explanations = new ArrayList<>();

        String type = transaction.getType();
        double amount = transaction.getAmount();
        double oldBalance = transaction.getOldBalance();
        double newBalance = transaction.getNewBalance();

        boolean highRiskType =
                "TRANSFER".equals(type)
                || "CASH_OUT".equals(type);

        if (highRiskType) {
            explanations.add(
                    "High-risk transaction type: " + type
            );
        }

        if (amount > 10000) {
            explanations.add(
                    "Transaction exceeds $10,000"
            );
        }

        if (amount > 100000) {
            explanations.add(
                    "Transaction exceeds $100,000"
            );
        }

        boolean drainsAccount =
                oldBalance > 0
                && Math.abs(oldBalance - amount) < 0.01
                && Math.abs(newBalance) < 0.01;

        if (highRiskType && drainsAccount) {
            explanations.add(
                    "Transaction drains the sender's balance"
            );
        }

        return explanations;
    }
        
        private void printTransactionHistory(
                String title,
                List<Transaction> transactions) {

            System.out.println("\n=== " + title + " ===");

            List<Transaction> history = new ArrayList<>();

            for (Transaction t : transactions) {

                // Exclude transactions from future time steps.
                if (t.getStep() <= transaction.getStep()) {
                    history.add(t);
                }
            }

            // Sort chronologically without modifying the graph.
            history.sort(
                    Comparator.comparingInt(Transaction::getStep)
            );

            if (history.isEmpty()) {
                System.out.println("No transactions found.");
                return;
            }

            for (Transaction t : history) {
                System.out.printf(
                        "Step %d | %s -> %s | $%,.2f%n",
                        t.getStep(),
                        t.getSender(),
                        t.getReceiver(),
                        t.getAmount()
                );
            }
        }

    
    private int countTransactionsUpToCurrentStep(
            List<Transaction> transactions) {

        int count = 0;

        for (Transaction t : transactions) {
            if (t.getStep() <= transaction.getStep()) {
                count++;
            }
        }

        return count;
    }


    public void printReport() {

        System.out.println(
                "\n=== GraphGuard Investigation Report ==="
        );

        System.out.println(
                "Transaction ID: "
                + transaction.getTransactionId()
        );

        System.out.println(
                "Sender: " + transaction.getSender()
        );

        System.out.println(
                "Receiver: " + transaction.getReceiver()
        );

        System.out.printf(
                "Amount: $%,.2f%n", transaction.getAmount()
        );

        System.out.println(
                "Risk Score: "
                + fraudService.calculateRiskScore(transaction)
        );

        System.out.println(
                "Risk Level: "
                + fraudService.evaluateTransaction(transaction)
        );

        System.out.println("\nRisk Explanations:");

        for (String explanation : getRiskExplanations()) {
            System.out.println("- " + explanation);
        }

        System.out.println("\nGraph Context:");

        
        System.out.println(
                "Sender outgoing transactions: "
                + countTransactionsUpToCurrentStep(
                        graph.getOutgoingTransactions(
                                transaction.getSender()
                        )
                )
        );

        System.out.println(
                "Receiver incoming transactions: "
                + countTransactionsUpToCurrentStep(
                        graph.getIncomingTransactions(
                                transaction.getReceiver()
                        )
                )
        );


        
        printTransactionHistory(
                "Sender Outgoing History",
                graph.getOutgoingTransactions(transaction.getSender())
        );

        printTransactionHistory(
                "Receiver Incoming History",
                graph.getIncomingTransactions(transaction.getReceiver())
        );

    }
}
