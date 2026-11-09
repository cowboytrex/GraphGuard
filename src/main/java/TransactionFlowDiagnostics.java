
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TransactionFlowDiagnostics {

    public static void main(String[] args) throws IOException {

        Set<String> transferReceivers = new HashSet<>();
        Set<String> cashOutSenders = new HashSet<>();

        long transfers = 0;
        long cashOuts = 0;

        try (BufferedReader reader = new BufferedReader(
                new FileReader("data/transactions.csv"))) {

            reader.readLine();

            String line;

            while ((line = reader.readLine()) != null) {

                String[] columns = line.split(",", -1);

                if (columns.length != 11) {
                    continue;
                }

                int step;

                try {
                    step = Integer.parseInt(columns[0]);
                } catch (NumberFormatException e) {
                    continue;
                }

                if (step > 500) {
                    break;
                }

                if (step < 1) {
                    continue;
                }

                String type = columns[1];
                String sender = columns[3];
                String receiver = columns[6];

                if (type.equals("TRANSFER")) {
                    transfers++;
                    transferReceivers.add(receiver);
                }

                if (type.equals("CASH_OUT")) {
                    cashOuts++;
                    cashOutSenders.add(sender);
                }
            }
        }

        int overlappingAccounts = 0;

        for (String account : transferReceivers) {
            if (cashOutSenders.contains(account)) {
                overlappingAccounts++;
            }
        }

        System.out.println("=== Transaction Flow Diagnostics ===");
        System.out.println("Transfers: " + transfers);
        System.out.println("Cash-outs: " + cashOuts);
        System.out.println(
                "Unique transfer receivers: "
                + transferReceivers.size()
        );
        System.out.println(
                "Unique cash-out senders: "
                + cashOutSenders.size()
        );
        System.out.println(
                "Overlapping accounts: "
                + overlappingAccounts
        );
    }
}
