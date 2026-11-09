
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class TransactionAmountDiagnostics {

    private static final String FILE_PATH =
            "data/transactions.csv";

    private static final int MAX_STEP = 500;
    private static final int WINDOW_STEPS = 24;

    private record Transfer(int step, double amount) {}

    public static void main(String[] args) throws IOException {

        Map<String, Deque<Transfer>> history =
                new HashMap<>();

        long below80 = 0;
        long between80And100 = 0;
        long above100 = 0;
        long qualifyingCashOuts = 0;
        long fraudulentCashOuts = 0;

        int lastStep = -1;
        int printedExamples = 0;

        try (BufferedReader reader =
                new BufferedReader(new FileReader(FILE_PATH))) {

            reader.readLine();

            String line;

            while ((line = reader.readLine()) != null) {

                String[] columns = line.split(",", -1);

                if (columns.length != 11) {
                    continue;
                }

                int step;
                double amount;
                int fraud;

                try {
                    step = Integer.parseInt(columns[0]);
                    amount = Double.parseDouble(columns[2]);
                    fraud = Integer.parseInt(columns[9]);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (step > MAX_STEP) {
                    break;
                }

                if (step < 1 || !Double.isFinite(amount)
                        || amount <= 0 || (fraud != 0 && fraud != 1)) {
                    continue;
                }

                if (step < lastStep) {
                    throw new IllegalStateException(
                            "CSV transactions are out of order"
                    );
                }

                lastStep = step;

                String type = columns[1];
                String sender = columns[3];
                String receiver = columns[6];

                if ("TRANSFER".equals(type)) {

                    history.computeIfAbsent(
                            receiver,
                            key -> new ArrayDeque<>()
                    ).addLast(new Transfer(step, amount));

                } else if ("CASH_OUT".equals(type)) {

                    Deque<Transfer> transfers = history.get(sender);

                    if (transfers == null) {
                        continue;
                    }

                    while (!transfers.isEmpty()
                            && transfers.peekFirst().step()
                                    <= step - WINDOW_STEPS) {
                        transfers.removeFirst();
                    }

                    if (transfers.isEmpty()) {
                        continue;
                    }

                    Transfer latest = transfers.peekLast();

                    if (latest.step() > step) {
                        continue;
                    }

                    double ratio = amount / latest.amount();

                    if (!Double.isFinite(ratio)) {
                        continue;
                    }

                    qualifyingCashOuts++;

                    if (fraud == 1) {
                        fraudulentCashOuts++;
                    }

                    if (ratio < 0.80) {
                        below80++;
                    } else if (ratio <= 1.00) {
                        between80And100++;
                    } else {
                        above100++;
                    }

                    if (printedExamples < 10) {
                        System.out.printf(
                                "Step %d | Transfer: %.2f"
                                + " | Cash-out: %.2f"
                                + " | Ratio: %.4f"
                                + " | Fraud: %d%n",
                                step,
                                latest.amount(),
                                amount,
                                ratio,
                                fraud
                        );
                        printedExamples++;
                    }
                }
            }
        }

        System.out.println();
        System.out.println("=== Transaction Amount Diagnostics ===");
        System.out.println("Qualifying cash-outs: " + qualifyingCashOuts);
        System.out.println("Fraudulent cash-outs: " + fraudulentCashOuts);
        System.out.println("Ratio below 0.80: " + below80);
        System.out.println("Ratio 0.80 to 1.00: " + between80And100);
        System.out.println("Ratio above 1.00: " + above100);
    }
}
