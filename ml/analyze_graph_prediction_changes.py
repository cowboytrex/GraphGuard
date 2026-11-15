
from pathlib import Path

import pandas as pd
from sklearn.ensemble import RandomForestClassifier

from graph_features import calculate_prior_unique_senders


PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_PATH = PROJECT_ROOT / "data" / "transactions.csv"

BASE_FEATURES = [
    "type",
    "amount",
    "oldbalanceOrg",
    "oldbalanceDest",
]

GRAPH_FEATURE = "prior_unique_senders_24h"
THRESHOLD = 0.80


def prepare_features(data, features, reference_columns=None):
    X = pd.get_dummies(
        data[features],
        columns=["type"],
        dtype="int8",
    )

    if reference_columns is not None:
        X = X.reindex(
            columns=reference_columns,
            fill_value=0,
        )

    return X


def train_and_predict(train, validation, features):
    X_train = prepare_features(train, features)
    X_val = prepare_features(
        validation,
        features,
        reference_columns=X_train.columns,
    )

    model = RandomForestClassifier(
        n_estimators=50,
        max_depth=12,
        min_samples_leaf=5,
        random_state=42,
        n_jobs=2,
    )

    model.fit(X_train, train["isFraud"])

    probabilities = model.predict_proba(X_val)[:, 1]

    return pd.Series(
        (probabilities >= THRESHOLD).astype(int),
        index=validation.index,
    )


def summarize_group(name, group):
    print(f"\n{name}")
    print("-" * 55)
    print(f"Transactions: {len(group):,}")

    if group.empty:
        return

    print("\nTransaction types:")
    print(group["type"].value_counts().to_string())

    print("\nHistorical sender counts:")
    print(
        group[GRAPH_FEATURE]
        .value_counts()
        .sort_index()
        .to_string()
    )

    print("\nMedian transaction amount:")
    print(f"{group['amount'].median():,.2f}")


def main():
    print("GRAPHGUARD - PAIRED GRAPH PREDICTION ANALYSIS")
    print("-" * 60)

    columns = [
        "step",
        "nameOrig",
        "nameDest",
        "isFraud",
    ] + BASE_FEATURES

    df = pd.read_csv(DATA_PATH, usecols=columns)

    # Exclude the held-out test period.
    df = df[df["step"] <= 354].copy()

    df = df.sort_values(
        "step",
        kind="stable",
    ).reset_index(drop=True)

    print(f"Transactions loaded: {len(df):,}")
    print("\nCalculating historical graph feature...")

    df[GRAPH_FEATURE] = calculate_prior_unique_senders(
        df,
        window=24,
    )

    train = df[df["step"] <= 299]
    validation = df[
        (df["step"] >= 300)
        & (df["step"] <= 354)
    ].copy()

    fraud = train[train["isFraud"] == 1]
    legitimate = train[
        train["isFraud"] == 0
    ].sample(n=100_000, random_state=42)

    training_sample = pd.concat(
        [fraud, legitimate],
        ignore_index=True,
    )

    print("\nTraining baseline model...")
    validation["baseline_prediction"] = train_and_predict(
        training_sample,
        validation,
        BASE_FEATURES,
    )

    print("Training graph-enhanced model...")
    validation["graph_prediction"] = train_and_predict(
        training_sample,
        validation,
        BASE_FEATURES + [GRAPH_FEATURE],
    )

    recovered_fraud = validation[
        (validation["isFraud"] == 1)
        & (validation["baseline_prediction"] == 0)
        & (validation["graph_prediction"] == 1)
    ]

    newly_missed_fraud = validation[
        (validation["isFraud"] == 1)
        & (validation["baseline_prediction"] == 1)
        & (validation["graph_prediction"] == 0)
    ]

    newly_flagged_legitimate = validation[
        (validation["isFraud"] == 0)
        & (validation["baseline_prediction"] == 0)
        & (validation["graph_prediction"] == 1)
    ]

    no_longer_flagged_legitimate = validation[
        (validation["isFraud"] == 0)
        & (validation["baseline_prediction"] == 1)
        & (validation["graph_prediction"] == 0)
    ]

    summarize_group(
        "RECOVERED FRAUD",
        recovered_fraud,
    )

    summarize_group(
        "NEWLY MISSED FRAUD",
        newly_missed_fraud,
    )

    summarize_group(
        "NEWLY FLAGGED LEGITIMATE",
        newly_flagged_legitimate,
    )

    summarize_group(
        "NO LONGER FLAGGED LEGITIMATE",
        no_longer_flagged_legitimate,
    )

    print("\nNET CHANGES")
    print("-" * 55)
    print(
        "True positive change:",
        len(recovered_fraud) - len(newly_missed_fraud),
    )
    print(
        "False positive change:",
        len(newly_flagged_legitimate)
        - len(no_longer_flagged_legitimate),
    )

    print("\nPaired prediction analysis complete!")


if __name__ == "__main__":
    main()
