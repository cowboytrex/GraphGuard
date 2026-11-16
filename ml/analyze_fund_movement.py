
from pathlib import Path

import pandas as pd

from fund_movement_features import calculate_prior_incoming_transfers


PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_PATH = PROJECT_ROOT / "data" / "transactions.csv"

WINDOW = 24


def summarize(name, data):
    print(f"\n{name}")
    print("-" * 55)

    total = len(data)
    active = data["prior_incoming_transfers_24h"] > 0
    matching = int(active.sum())

    print(f"Transactions: {total:,}")
    print(f"With prior incoming transfers: {matching:,}")
    print(f"Matching rate: {matching / total:.4%}")

    print("\nIncoming transfer count distribution:")
    print(
        data["prior_incoming_transfers_24h"]
        .value_counts()
        .sort_index()
        .head(15)
        .to_string()
    )

    if matching:
        print("\nFor transactions with prior incoming transfers:")
        print(
            "Median prior incoming amount:",
            f"{data.loc[active, 'prior_incoming_amount_24h'].median():,.2f}",
        )

        ratio = (
            data.loc[active, "prior_incoming_amount_24h"]
            / data.loc[active, "amount"].replace(0, float("nan"))
        )

        print(f"Median incoming-to-outgoing ratio: {ratio.median():.4f}")


def main():
    print("GRAPHGUARD - RAPID FUND MOVEMENT ANALYSIS")
    print("-" * 60)

    df = pd.read_csv(
        DATA_PATH,
        usecols=[
            "step",
            "type",
            "amount",
            "nameOrig",
            "nameDest",
            "isFraud",
        ],
    )

    # Exclude held-out test data.
    df = df[df["step"] <= 354].copy()
    df = df.sort_values("step", kind="stable").reset_index(drop=True)

    print(f"Transactions loaded: {len(df):,}")
    print("\nCalculating historical incoming transfers...")

    features = calculate_prior_incoming_transfers(
        df,
        window=WINDOW,
    )

    df = pd.concat([df, features], axis=1)

    validation = df[
        (df["step"] >= 300)
        & (df["step"] <= 354)
        & (df["type"] == "CASH_OUT")
    ]

    summarize("ALL VALIDATION CASH_OUT", validation)

    summarize(
        "FRAUDULENT CASH_OUT",
        validation[validation["isFraud"] == 1],
    )

    summarize(
        "LEGITIMATE CASH_OUT",
        validation[validation["isFraud"] == 0],
    )

    print("\nRapid fund movement analysis complete!")


if __name__ == "__main__":
    main()
