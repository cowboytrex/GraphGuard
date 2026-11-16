
from pathlib import Path

import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import confusion_matrix


PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_PATH = PROJECT_ROOT / "data" / "transactions.csv"

FEATURE_COLUMNS = [
    "type",
    "amount",
    "oldbalanceOrg",
    "newbalanceOrig",
    "oldbalanceDest",
    "newbalanceDest",
]

THRESHOLD = 0.80


def main():
    print("GRAPHGUARD - FALSE NEGATIVE ANALYSIS")
    print("-" * 55)

    df = pd.read_csv(
        DATA_PATH,
        usecols=["step", "isFraud"] + FEATURE_COLUMNS,
    )

    # Restrict analysis to training and validation.
    df = df[df["step"] <= 354]

    train_df = df[df["step"] <= 299]
    val_df = df[
        (df["step"] >= 300)
        & (df["step"] <= 354)
    ].copy()

    # Reproduce baseline sampling exactly.
    train_fraud = train_df[train_df["isFraud"] == 1]

    train_legitimate = train_df[
        train_df["isFraud"] == 0
    ].sample(
        n=100_000,
        random_state=42,
    )

    train_sample = pd.concat(
        [train_fraud, train_legitimate],
        ignore_index=True,
    )

    X_train = pd.get_dummies(
        train_sample[FEATURE_COLUMNS],
        columns=["type"],
        dtype="int8",
    )

    X_val = pd.get_dummies(
        val_df[FEATURE_COLUMNS],
        columns=["type"],
        dtype="int8",
    )

    X_val = X_val.reindex(
        columns=X_train.columns,
        fill_value=0,
    )

    y_train = train_sample["isFraud"]
    y_val = val_df["isFraud"]

    model = RandomForestClassifier(
        n_estimators=50,
        max_depth=12,
        min_samples_leaf=5,
        random_state=42,
        n_jobs=2,
    )

    print("\nTraining baseline Random Forest...")
    model.fit(X_train, y_train)

    probabilities = model.predict_proba(X_val)[:, 1]
    predictions = (probabilities >= THRESHOLD).astype(int)

    val_df["fraud_probability"] = probabilities
    val_df["prediction"] = predictions

    tn, fp, fn, tp = confusion_matrix(
        y_val,
        predictions,
        labels=[0, 1],
    ).ravel()

    print("\nVALIDATION CONFUSION MATRIX")
    print("-" * 55)
    print(f"True positives: {tp:,}")
    print(f"False positives: {fp:,}")
    print(f"True negatives: {tn:,}")
    print(f"False negatives: {fn:,}")

    # Separate detected and missed fraud.
    detected = val_df[
        (val_df["isFraud"] == 1)
        & (val_df["prediction"] == 1)
    ]

    missed = val_df[
        (val_df["isFraud"] == 1)
        & (val_df["prediction"] == 0)
    ]

    print("\nFRAUD BY TRANSACTION TYPE")
    print("-" * 55)

    for tx_type in ["TRANSFER", "CASH_OUT"]:
        detected_count = (detected["type"] == tx_type).sum()
        missed_count = (missed["type"] == tx_type).sum()
        total = detected_count + missed_count

        print(f"\n{tx_type}")
        print(f"Detected: {detected_count:,}")
        print(f"Missed: {missed_count:,}")

        if total > 0:
            print(f"Recall: {detected_count / total:.4%}")

    print("\nTRANSACTION AMOUNT COMPARISON")
    print("-" * 55)

    for name, subset in [
        ("Detected fraud", detected),
        ("Missed fraud", missed),
    ]:
        print(f"\n{name}")
        print(f"Count: {len(subset):,}")
        print(f"Median amount: {subset['amount'].median():,.2f}")
        print(f"Mean amount: {subset['amount'].mean():,.2f}")
        print(f"Minimum amount: {subset['amount'].min():,.2f}")
        print(f"Maximum amount: {subset['amount'].max():,.2f}")

    print("\nMISSED FRAUD PROBABILITY DISTRIBUTION")
    print("-" * 55)

    probability_bins = [
        0.0, 0.1, 0.2, 0.3, 0.4,
        0.5, 0.6, 0.7, 0.8,
    ]

    missed_bins = pd.cut(
        missed["fraud_probability"],
        bins=probability_bins,
        include_lowest=True,
    )

    print(
        missed_bins.value_counts()
        .sort_index()
        .to_string()
    )

    print("\nORIGIN ACCOUNT BALANCE BEHAVIOR")
    print("-" * 55)

    for name, subset in [
        ("Detected fraud", detected),
        ("Missed fraud", missed),
    ]:
        count = len(subset)

        zero_before = (
            subset["oldbalanceOrg"] == 0
        ).sum()

        zero_after = (
            subset["newbalanceOrig"] == 0
        ).sum()

        print(f"\n{name}")
        print(
            f"Zero origin balance before: "
            f"{zero_before:,} / {count:,}"
        )
        print(
            f"Zero origin balance after: "
            f"{zero_after:,} / {count:,}"
        )

    print("\nFalse negative analysis complete!")


if __name__ == "__main__":
    main()
