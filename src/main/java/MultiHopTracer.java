
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultiHopTracer {

    public List<List<Transaction>> trace(
            List<Transaction> transactions,
            String startAccount,
            int maxHops) {

        List<List<Transaction>> paths = new ArrayList<>();

        if (maxHops <= 0) {
            return paths;
        }

        Map<String, List<Transaction>> outgoing = new HashMap<>();

        for (Transaction t : transactions) {

            if ("TRANSFER".equals(t.getType())
                    && t.getAmount() > 0) {

                outgoing.computeIfAbsent(
                        t.getSender(),
                        key -> new ArrayList<>()
                ).add(t);
            }
        }

        Set<String> visited = new HashSet<>();
        visited.add(startAccount);

        dfs(startAccount, -1, maxHops,
                outgoing, visited,
                new ArrayList<>(), paths);

        return paths;
    }

    private void dfs(
            String account,
            int previousStep,
            int maxHops,
            Map<String, List<Transaction>> outgoing,
            Set<String> visited,
            List<Transaction> currentPath,
            List<List<Transaction>> paths) {

        if (currentPath.size() >= maxHops) {
            return;
        }

        for (Transaction t :
                outgoing.getOrDefault(account, List.of())) {

            if (t.getStep() <= previousStep) {
                continue;
            }

            String nextAccount = t.getReceiver();

            if (visited.contains(nextAccount)) {
                continue;
            }

            currentPath.add(t);
            visited.add(nextAccount);

            paths.add(new ArrayList<>(currentPath));

            dfs(nextAccount, t.getStep(), maxHops,
                    outgoing, visited, currentPath, paths);

            visited.remove(nextAccount);
            currentPath.remove(currentPath.size() - 1);
        }
    }
}
