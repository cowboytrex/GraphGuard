import pandas as pd

from preauth_baseline import train_and_score_validation, THRESHOLD


def label_groups(c):
    group = pd.Series("legit", index=c.index)
    fraud = c["isFraud"] == 1
    group[fraud & (c["prediction"] == 1)] = "fraud_caught"
    group[fraud & (c["prediction"] == 0)] = "fraud_missed"
    return group


def main():
    print("GRAPHGUARD - MISSED CASH_OUT FRAUD DIAGNOSTIC")
    print("-" * 60)

    validation = train_and_score_validation()
    c = validation[validation["type"] == "CASH_OUT"].copy()
    c["group"] = label_groups(c)

    # Drains full balance: amount equals the sender's balance to the cent.
    c["full_drain"] = (c["oldbalanceOrg"] > 0) & (
        (c["amount"] - c["oldbalanceOrg"]).abs() <= 0.01
    )
    c["exceeds_balance"] = c["amount"] > c["oldbalanceOrg"] + 0.01
    c["zero_sender_balance"] = c["oldbalanceOrg"] == 0
    c["zero_dest_balance"] = c["oldbalanceDest"] == 0

    print("\n1. PROFILE BY GROUP (tests H1 and H2)")
    print("-" * 60)
    summary = c.groupby("group").agg(
        rows=("amount", "size"),
        median_amount=("amount", "median"),
        median_sender_balance=("oldbalanceOrg", "median"),
        median_dest_balance=("oldbalanceDest", "median"),
        pct_full_drain=("full_drain", "mean"),
        pct_exceeds_balance=("exceeds_balance", "mean"),
        pct_zero_sender=("zero_sender_balance", "mean"),
        pct_zero_dest=("zero_dest_balance", "mean"),
        median_probability=("probability", "median"),
    )
    pct_cols = [col for col in summary.columns if col.startswith("pct_")]
    summary[pct_cols] = (100 * summary[pct_cols]).round(1)
    print(summary.round(2).T.to_string())

    print(f"\n2. SCORES OF MISSED FRAUD (tests H3; threshold = {THRESHOLD})")
    print("-" * 60)
    missed = c[c["group"] == "fraud_missed"]
    bins = [0, 0.1, 0.3, 0.5, THRESHOLD]
    print(
        pd.cut(missed["probability"], bins=bins, include_lowest=True)
        .value_counts()
        .sort_index()
        .to_string()
    )

    print("\nMissed CASH_OUT diagnostic complete!")


if __name__ == "__main__":
    main()