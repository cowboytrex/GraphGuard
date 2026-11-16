
from collections import defaultdict, deque
from pathlib import Path

import pandas as pd


PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_PATH = PROJECT_ROOT / "data" / "transactions.csv"

WINDOW = 24
TRANSACTION_TYPES = {"TRANSFER", "CASH_OUT"}


def calculate_prior_outgoing(transactions, window=24):
    """
    Count previous outgoing transactions per sender.

    Only transactions from earlier steps are counted.
    The current step is excluded.
    """
    history = defaultdict(deque)
    results = [0] * len(transactions)

    for step, group in transactions.groupby("step", sort=True):
        # First calculate counts without adding current-step activity.
        for position, sender in zip(
            group.index,
            group["nameOrig"],
        ):
            account_history = history[sender]

            while (
                account_history
                and account_history[0] < step - window
            ):
                account_history.popleft()

            results[position] = len(account_history)

        # Then add transactions from the current step.
        for sender, transaction_type in zip(
            group["nameOrig"],
            group["type"],
        ):
            if transaction_type in TRANSACTION_TYPES:
                history[sender].append(step)

    return results


def summarize(name, data):
    print(f"\n{name}")
    print("-" * 55)

    total = len(data)
    repeated = int((data["prior_outgoing_24h"] > 0).sum())

    print(f"Transactions: {total:,}")
    print(f"With previous outgoing activity: {repeated:,}")
    print(f"Repeated activity rate: {repeated / total:.4%}")
    print(
        "Maximum prior outgoing count:",
        data["prior_outgoing_24h"].max(),
    )

    print("\nPrior outgoing count distribution:")
    print(
        data["prior_outgoing_24h"]
        .value_counts()
        .sort_index()
        .head(15)
        .to_string()
    )


def main():
    print("GRAPHGUARD - TRANSACTION VELOCITY ANALYSIS")
    print("-" * 60)

    df = pd.read_csv(
        DATA_PATH,
        usecols=[
            "step",
            "type",
            "nameOrig",
            "isFraud",
        ],
    )

    # Training and validation only.
    df = df[df["step"] <= 354].copy()

    df = df.sort_values(
        "step",
        kind="stable",
    ).reset_index(drop=True)

    print(f"Transactions loaded: {len(df):,}")

    print("\nCalculating historical outgoing activity...")

    df["prior_outgoing_24h"] = calculate_prior_outgoing(
        df,
        window=WINDOW,
    )

    validation = df[
        (df["step"] >= 300)
        & (df["step"] <= 354)
        & (df["type"].isin(TRANSACTION_TYPES))
    ]

    summarize("ALL VALIDATION TRANSACTIONS", validation)

    for transaction_type in ["TRANSFER", "CASH_OUT"]:
        subset = validation[
            validation["type"] == transaction_type
        ]

        summarize(
            f"{transaction_type} - FRAUD",
            subset[subset["isFraud"] == 1],
        )

        summarize(
            f"{transaction_type} - LEGITIMATE",
            subset[subset["isFraud"] == 0],
        )

    print("\nTransaction velocity analysis complete!")


if __name__ == "__main__":
    main()
