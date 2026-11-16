
from pathlib import Path

import pandas as pd

from graph_features import calculate_prior_unique_senders


PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_PATH = PROJECT_ROOT / "data" / "transactions.csv"

GRAPH_FEATURE = "prior_unique_senders_24h"


def summarize(name, data):
    print(f"\n{name}")
    print("-" * 55)

    total = len(data)
    nonzero = int((data[GRAPH_FEATURE] > 0).sum())

    print(f"Transactions: {total:,}")
    print(f"Nonzero graph feature: {nonzero:,}")
    print(f"Nonzero rate: {nonzero / total:.4%}")
    print(f"Mean: {data[GRAPH_FEATURE].mean():.4f}")
    print(f"Median: {data[GRAPH_FEATURE].median():.4f}")
    print(f"Maximum: {data[GRAPH_FEATURE].max()}")

    print("\nValue distribution:")
    print(
        data[GRAPH_FEATURE]
        .value_counts()
        .sort_index()
        .head(15)
        .to_string()
    )


def main():
    print("GRAPHGUARD - GRAPH FEATURE DISTRIBUTION")
    print("-" * 60)

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

    # Training and validation only.
    df = df[df["step"] <= 354].copy()

    df = df.sort_values(
        "step",
        kind="stable",
    ).reset_index(drop=True)

    print(f"Transactions loaded: {len(df):,}")
    print("\nCalculating historical sender counts...")

    df[GRAPH_FEATURE] = calculate_prior_unique_senders(
        df,
        window=24,
    )

    validation = df[
        (df["step"] >= 300)
        & (df["step"] <= 354)
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

    print("\nGraph feature distribution analysis complete!")


if __name__ == "__main__":
    main()
