
from pathlib import Path

import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    confusion_matrix,
    precision_score,
    recall_score,
    f1_score,
)

from balance_features import calculate_exact_depletion


PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_PATH = PROJECT_ROOT / "data" / "transactions.csv"

BASE_FEATURES = [
    "type",
    "amount",
    "oldbalanceOrg",
    "newbalanceOrig",
    "oldbalanceDest",
    "newbalanceDest",
]

BALANCE_FEATURE = "exact_depletion"
THRESHOLD = 0.80


def prepare_features(data, columns, reference_columns=None):
    X = pd.get_dummies(
        data[columns],
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
        group_predictions = predictions.loc[subset.index]
        y_true = subset["isFraud"]

        tn, fp, fn, tp = confusion_matrix(
            y_true,
            group_predictions,
            labels=[0, 1],
        ).ravel()

        precision = precision_score(
            y_true, group_predictions, zero_division=0
        )
        recall = recall_score(
            y_true, group_predictions, zero_division=0
        )
        f1 = f1_score(
            y_true, group_predictions, zero_division=0
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
    print("GRAPHGUARD - BALANCE FEATURE EXPERIMENT")
    print("-" * 60)

    columns = ["step", "isFraud"] + BASE_FEATURES

    df = pd.read_csv(
        DATA_PATH,
        usecols=columns,
    )

    # Exclude the held-out test period.
    df = df[df["step"] <= 354].copy()

    df[BALANCE_FEATURE] = calculate_exact_depletion(df)

    train_df = df[df["step"] <= 299]
    val_df = df[
        (df["step"] >= 300)
        & (df["step"] <= 354)
    ].copy()

    train_fraud = train_df[train_df["isFraud"] == 1]
    train_legitimate = train_df[
        train_df["isFraud"] == 0
    ].sample(n=100_000, random_state=42)

    train_sample = pd.concat(
        [train_fraud, train_legitimate],
        ignore_index=True,
    )

    print(f"Training sample: {len(train_sample):,}")
    print(f"Validation transactions: {len(val_df):,}")

    experiments = [
        ("BASELINE RANDOM FOREST", BASE_FEATURES),
        (
            "BALANCE-ENHANCED RANDOM FOREST",
            BASE_FEATURES + [BALANCE_FEATURE],
        ),
    ]

    for name, features in experiments:
        print(f"\nTraining {name}...")

        X_train = prepare_features(
            train_sample,
            features,
        )
        X_val = prepare_features(
            val_df,
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

        model.fit(X_train, train_sample["isFraud"])

        probabilities = model.predict_proba(X_val)[:, 1]
        predictions = pd.Series(
            (probabilities >= THRESHOLD).astype(int),
            index=val_df.index,
        )

        evaluate(name, val_df, predictions)

        if BALANCE_FEATURE in X_train.columns:
            feature_index = X_train.columns.get_loc(
                BALANCE_FEATURE
            )
            print(
                "\nExact depletion feature importance:",
                f"{model.feature_importances_[feature_index]:.6f}",
            )

    # Deterministic rule: flags only exact-depletion CASH_OUT.
    rule_predictions = val_df[BALANCE_FEATURE].astype(int)

    evaluate(
        "EXACT-DEPLETION RULE",
        val_df,
        rule_predictions,
    )

    print("\nBalance feature experiment complete!")


if __name__ == "__main__":
    main()
