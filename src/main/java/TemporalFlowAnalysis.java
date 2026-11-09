import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TemporalFlowAnalysis {

    private static final String FILE_PATH =
         "data/transactions.csv";

    private static final int MAX_STEP = 500;
    private static final int WINDOW_STEPS = 24;
    private static final double AMOUNT_RATIO = 0.80;

    private record Transfer(int step, double amount) {}

    public static void main(String[] args) throws IOException {

        Map<String, Deque<Transfer>> history=
                new HashMap<>();

        Set<String> chronologicalAccounts = new HashSet<>();
        Set<String> withinWindowAccounts = new HashSet<>();
        Set<String> matchingAccounts = new HashSet<>();

        long chronologicalCashOuts = 0;
        long withinWindowCashOuts = 0;
        long matchingCashOuts = 0;

        int lastStep = -1;

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

                try {
                    step = Integer.parseInt(columns[0]);
                    amount = Double.parseDouble(columns[2]);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (step > MAX_STEP) {
                    break;
                }

                if (step < 1) {
                    continue;
                }

                if (step < lastStep) {
                    throw new IllegalStateException(
                            "Transactions are not chronologically ordered"
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

                    boolean chronological = false;
                    boolean withinWindow = false;
                    boolean amountMatch = false;

                    for (Transfer transfer : transfers) {

                        int difference = step - transfer.step();

                        if (difference < 0) {
                            continue;
                        }

                        chronological = true;

                        if (difference >= WINDOW_STEPS) {
                            continue;
                        }

                        withinWindow = true;

                        if (transfer.amount() > 0
                                && amount >= transfer.amount() * AMOUNT_RATIO
                                && amount <= transfer.amount()) {

                            amountMatch = true;
                        }
                    }

                    if (chronological) {
                        chronologicalCashOuts++;
                        chronologicalAccounts.add(sender);
                    }

                    if (withinWindow) {
                        withinWindowCashOuts++;
                        withinWindowAccounts.add(sender);
                    }

                    if (amountMatch) {
                        matchingCashOuts++;
                        matchingAccounts.add(sender);
                    }
                }
            }
        }

        System.out.println("=== Temporal Flow Analysis ===");

        print("Transfer-before-cash-out",
                chronologicalAccounts.size(),
                chronologicalCashOuts);

        print("Within 24 steps",
                withinWindowAccounts.size(),
                withinWindowCashOuts);

        print("Matching 80%-100% amount",
                matchingAccounts.size(),
                matchingCashOuts);
    }

    private static void print(
            String label,
            int accounts,
            long cashOuts) {

        System.out.println();
        System.out.println(label);
        System.out.println("Unique accounts: " + accounts);
        System.out.println("Cash-out transactions: " + cashOuts);
    }
}