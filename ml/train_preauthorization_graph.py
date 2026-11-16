
from pathlib import Path

import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    confusion_matrix,
    precision_score,
    recall_score,
    f1_score,
    average_precision_score,
)

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


def evaluate(name, data, predictions):
    print(f"\n{name}")
    print("-" * 60)

    for group_name, subset in [
        ("ALL TRANSACTIONS", data),
        ("TRANSFER", data[data["type"] == "TRANSFER"]),
        ("CASH_OUT", data[data["type"] == "CASH_OUT"]),
    ]:
        y_true = subset["isFraud"]
        group_predictions = predictions.loc[subset.index]

        tn, fp, fn, tp = confusion_matrix(
            y_true,
            group_predictions,
            labels=[0, 1],
        ).ravel()

        precision = precision_score(
            y_true,
            group_predictions,
            zero_division=0,
        )
        recall = recall_score(
            y_true,
            group_predictions,
            zero_division=0,
        )
        f1 = f1_score(
            y_true,
            group_predictions,
            zero_division=0,
        )

        print(f"\n{group_name}")
        print(f"TP: {tp:,} | FP: {fp:,}")
        print(f"TN: {tn:,} | FN: {fn:,}")
        print(
            f"Precision: {precision:.4f} | "
            f"Recall: {recall:.4f} | "
            f"F1: {f1:.4f}"
        )


def main():
    print("GRAPHGUARD - PREAUTHORIZATION GRAPH EXPERIMENT")
    print("-" * 60)

    columns = [
        "step",
        "nameOrig",
        "nameDest",
        "isFraud",
    ] + BASE_FEATURES

    df = pd.read_csv(
        DATA_PATH,
        usecols=columns,
    )

    # Keep only training and validation periods.
    df = df[df["step"] <= 354].copy()

    # Preserve chronological order.
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
    ].sample(
        n=100_000,
        random_state=42,
    )

    training_sample = pd.concat(
        [fraud, legitimate],
        ignore_index=True,
    )

    print(f"Training sample: {len(training_sample):,}")
    print(f"Validation transactions: {len(validation):,}")

    experiments = [
        ("PREAUTHORIZATION BASELINE", BASE_FEATURES),
        (
            "GRAPH-ENHANCED PREAUTHORIZATION",
            BASE_FEATURES + [GRAPH_FEATURE],
        ),
    ]

    for name, features in experiments:
        print(f"\nTraining {name}...")

        X_train = prepare_features(
            training_sample,
            features,
        )

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

        model.fit(
            X_train,
            training_sample["isFraud"],
        )

        probabilities = model.predict_proba(X_val)[:, 1]

        predictions = pd.Series(
            (probabilities >= THRESHOLD).astype(int),
            index=validation.index,
        )

        evaluate(name, validation, predictions)

        ap = average_precision_score(
            validation["isFraud"],
            probabilities,
        )

        print(f"\nAverage precision (AP): {ap:.4f}")

        if GRAPH_FEATURE in X_train.columns:
            feature_index = X_train.columns.get_loc(
                GRAPH_FEATURE
            )

            print(
                "Graph feature importance:",
                f"{model.feature_importances_[feature_index]:.6f}",
            )

    print("\nGraph experiment complete!")


if __name__ == "__main__":
    main()
