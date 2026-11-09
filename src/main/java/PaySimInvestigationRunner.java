
import java.io.IOException;
import java.util.List;

public class PaySimInvestigationRunner {

    public static void main(String[] args) {

        TransactionDataLoader loader =
                new TransactionDataLoader();

        FraudDetectionService fraudService =
                new FraudDetectionService();

        TransactionGraph graph =
                new TransactionGraph();

        try {
            List<Transaction> transactions =
                    loader.loadTransactions(
                            "data/transactions.csv", 100000
                    );

            System.out.println(
                    "Loaded transactions: " + transactions.size()
            );

            int reportsGenerated = 0;

            for (Transaction transaction : transactions) {

                // Add the current transaction before investigation.
                graph.addTransaction(transaction);

                if ("HIGH RISK".equals(
                        fraudService.evaluateTransaction(transaction))) {

                    InvestigationReport report =
                            new InvestigationReport(
                                    transaction,
                                    fraudService,
                                    graph
                            );


                    report.printReport();

                    System.out.println(
                            "\nPaySim Ground Truth: "
                            + (transaction.isFraud()
                                    ? "FRAUD"
                                    : "LEGITIMATE")
                    );

                    reportsGenerated++;


                    // Limit output to the first three reports.
                    if (reportsGenerated >= 3) {
                        break;
                    }
                }
            }

            System.out.println(
                    "\nInvestigation reports generated: "
                    + reportsGenerated
            );

        } catch (IOException e) {
            System.out.println(
                    "Error loading PaySim data: " + e.getMessage()
            );
        }
    }
}
