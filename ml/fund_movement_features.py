
from collections import defaultdict, deque

import pandas as pd


def calculate_prior_incoming_transfers(transactions, window=24):
    """
    Calculate historical incoming TRANSFER activity.

    For each transaction, return:
        - Number of incoming transfers received by nameOrig.
        - Total amount received by nameOrig.

    Only earlier steps are included.
    Same-step transactions are excluded.

    Input must be sorted by step and have a RangeIndex.
    """

    required = {
        "step",
        "type",
        "amount",
        "nameOrig",
        "nameDest",
    }

    missing = required - set(transactions.columns)

    if missing:
        raise ValueError(f"Missing columns: {sorted(missing)}")

    if not transactions["step"].is_monotonic_increasing:
        raise ValueError("Transactions must be sorted by step.")

    if not isinstance(transactions.index, pd.RangeIndex):
        raise ValueError("Transactions must have a RangeIndex.")

    history = defaultdict(deque)

    counts = [0] * len(transactions)
    amounts = [0.0] * len(transactions)

    for step, group in transactions.groupby("step", sort=True):

        # Read history before adding any current-step transfers.
        for position, account in zip(
            group.index,
            group["nameOrig"],
        ):
            account_history = history.get(account)

            if not account_history:
                continue

            while (
                account_history
                and account_history[0][0] < step - window
            ):
                account_history.popleft()

            counts[position] = len(account_history)
            amounts[position] = sum(
                amount
                for _, amount in account_history
            )

        # Update history only after scoring the entire step.
        transfers = group[group["type"] == "TRANSFER"]

        for receiver, amount in zip(
            transfers["nameDest"],
            transfers["amount"],
        ):
            history[receiver].append(
                (step, float(amount))
            )

    return pd.DataFrame(
        {
            "prior_incoming_transfers_24h": counts,
            "prior_incoming_amount_24h": amounts,
        },
        index=transactions.index,
    )
