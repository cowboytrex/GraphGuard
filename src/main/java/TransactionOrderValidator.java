import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TransactionOrderValidator {

    public static void main(String[] args) throws IOException {

        String filePath = "data/transactions.csv";

        long checked = 0;
        long outOfOrder = 0;
        int previousStep = -1;

        try (BufferedReader reader =
                new BufferedReader(new FileReader(filePath))) {

            reader.readLine(); // Skip header

            String line;

            while ((line = reader.readLine()) != null) {

                String[] columns = line.split(",", -1);

                if (columns.length != 11) {
                    continue;
                }

                try {
                    int step = Integer.parseInt(columns[0]);

                    if (step < previousStep) {
                        outOfOrder++;
                    }

                    previousStep = step;
                    checked++;

                } catch (NumberFormatException e) {
                    // Skip invalid step values
                }
            }
        }

        System.out.println("=== Transaction Order Validation ===");
        System.out.println("Transactions checked: " + checked);
        System.out.println("Out-of-order transactions: " + outOfOrder);

        if (outOfOrder == 0) {
            System.out.println("PASS: Transactions are chronologically ordered.");
        } else {
            System.out.println("WARNING: Transactions are not fully ordered.");
        }
    }
}