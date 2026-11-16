
import time
import pandas as pd

from graph_features import calculate_prior_unique_senders


DATA_PATH = "data/transactions.csv"


def analyze_graph_features():
    print("GRAPHGUARD - GRAPH FEATURE FRAUD ANALYSIS")
    print("-" * 55)

    start = time.perf_counter()

    df = pd.read_csv(
        DATA_PATH,
        usecols=[
            "step",
            "type",
            "nameOrig",
            "nameDest",
            "isFraud",
        ],
    )

    print(f"Transactions loaded: {len(df):,}")

    # Preserve chronological ordering.
    df = df.sort_values("step", kind="stable").reset_index(drop=True)

    print("\nCalculating historical graph features...")

    df["prior_unique_senders_24h"] = (
        calculate_prior_unique_senders(df, window=24)
    )

    print("\nGRAPH FEATURE DISTRIBUTION")
    print("-" * 55)

    print(
        df.groupby("isFraud")["prior_unique_senders_24h"]
        .agg(["count", "mean", "median", "max"])
    )

    print("\nFRAUD RATE BY HISTORICAL SENDER COUNT")
    print("-" * 55)

    df["sender_group"] = pd.cut(
        df["prior_unique_senders_24h"],
        bins=[-1, 0, 1, 2, 5, 10, float("inf")],
        labels=[
            "0",
            "1",
            "2",
            "3-5",
            "6-10",
            "11+",
        ],
    )

    summary = df.groupby(
        "sender_group",
        observed=True,
    )["isFraud"].agg(
        transactions="count",
        fraud_cases="sum",
        fraud_rate="mean",
    )

    summary["fraud_rate"] *= 100

    print(summary)

    print("\nFRAUD TRANSACTIONS WITH HISTORICAL SENDERS")
    print("-" * 55)

    fraud = df[df["isFraud"] == 1]

    print(
        "Fraud cases with >= 1 prior sender:",
        int((fraud["prior_unique_senders_24h"] >= 1).sum()),
    )

    print(
        "Fraud cases with >= 3 prior senders:",
        int((fraud["prior_unique_senders_24h"] >= 3).sum()),
    )

    
    # STEP 291 - FRAUD ANALYSIS BY TRANSACTION TYPE

    print("\nFRAUD PATTERNS BY TRANSACTION TYPE")
    print("-" * 55)

    # PaySim labels fraud only in TRANSFER and CASH_OUT.
    fraud_types = ["TRANSFER", "CASH_OUT"]

    for tx_type in fraud_types:
        subset = df[df["type"] == tx_type]

        print(f"\nTRANSACTION TYPE: {tx_type}")
        print("-" * 45)

        print(f"Total transactions: {len(subset):,}")
        print(f"Fraud cases: {int(subset['isFraud'].sum()):,}")

        print("\nHistorical sender distribution:")

        distribution = (
            subset.groupby("isFraud")["prior_unique_senders_24h"]
            .agg(["count", "mean", "median", "max"])
        )

        print(distribution)

        print("\nFraud rate by sender group:")

        summary = (
            subset.groupby("sender_group", observed=True)["isFraud"]
            .agg(
                transactions="count",
                fraud_cases="sum",
                fraud_rate="mean",
            )
        )

        summary["fraud_rate"] *= 100

        print(summary)

        fraud_subset = subset[subset["isFraud"] == 1]

        zero_senders = (
            fraud_subset["prior_unique_senders_24h"] == 0
        ).sum()

        print(
            "\nFraud cases with zero historical senders:",
            int(zero_senders),
        )

        print(
            "Percentage of fraud with zero historical senders:",
            f"{zero_senders / len(fraud_subset) * 100:.2f}%"
        )


    elapsed = time.perf_counter() - start

    print(f"\nTotal analysis time: {elapsed:.2f} seconds")
    print("Graph feature fraud analysis complete!")


if __name__ == "__main__":
    analyze_graph_features()
