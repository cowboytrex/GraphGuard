
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TransactionDataLoader {

    public List<Transaction> loadTransactions(
            String filePath, int limit) throws IOException {

        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader =
                new BufferedReader(new FileReader(filePath))) {

            reader.readLine(); // Skip CSV header

            String line;
            int rowNumber = 0;

            while ((line = reader.readLine()) != null
                    && transactions.size() < limit) {

                rowNumber++;

                String[] columns = line.split(",", -1);

                if (columns.length != 11) {
                    continue;
                }

                try {
                    int step = Integer.parseInt(columns[0]);
                    String type = columns[1];

                    String transactionId = "TX" + rowNumber;
                    String sender = columns[3];
                    String receiver = columns[6];

                    double amount = Double.parseDouble(columns[2]);
                    double oldBalance = Double.parseDouble(columns[4]);
                    double newBalance = Double.parseDouble(columns[5]);

                    boolean isFraud = Integer.parseInt(columns[9]) == 1;

                    if (!Double.isFinite(amount) || amount < 0
                        || !Double.isFinite(oldBalance)
                        || !Double.isFinite(newBalance)
                        || oldBalance < 0
                        || newBalance < 0) {
                    continue;
                }   

                    Transaction transaction = new Transaction(
                        transactionId,
                        sender,
                        receiver,
                        amount,
                        isFraud,
                        type,
                        oldBalance,
                        newBalance,
                        step
                    );

                    transactions.add(transaction);

                } catch (NumberFormatException e) {
                    System.out.println(
                            "Skipping invalid row: " + rowNumber
                    );
                }
            }
        }

        return transactions;
    }
    
    public List<Transaction> loadTransactionsByStep(
            String filePath, int minStep, int maxStep) throws IOException {

        if (minStep > maxStep) {
            throw new IllegalArgumentException(
                    "minStep cannot be greater than maxStep"
            );
        }

        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader reader =
                new BufferedReader(new FileReader(filePath))) {

            reader.readLine(); // Skip CSV header

            String line;
            int rowNumber = 0;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                String[] columns = line.split(",", -1);

                if (columns.length != 11) {
                    continue;
                }

                try {
                    int step = Integer.parseInt(columns[0]);

                    if (step < minStep || step > maxStep) {
                        continue;
                    }

                    String type = columns[1];
                    double amount = Double.parseDouble(columns[2]);
                    String sender = columns[3];
                    double oldBalance = Double.parseDouble(columns[4]);
                    double newBalance = Double.parseDouble(columns[5]);
                    String receiver = columns[6];
                    int fraudLabel = Integer.parseInt(columns[9]);

                    if (!Double.isFinite(amount) || amount < 0
                            || !Double.isFinite(oldBalance)
                            || !Double.isFinite(newBalance)
                            || oldBalance < 0
                            || newBalance < 0
                            || (fraudLabel != 0 && fraudLabel != 1)) {
                        continue;
                    }

                    Transaction transaction = new Transaction(
                            "TX" + rowNumber,
                            sender,
                            receiver,
                            amount,
                            fraudLabel == 1,
                            type,
                            oldBalance,
                            newBalance,
                            step
                    );

                    transactions.add(transaction);

                } catch (NumberFormatException e) {
                    System.out.println(
                            "Skipping invalid row: " + rowNumber
                    );
                }
            }
        }

        return transactions;
    }

}
