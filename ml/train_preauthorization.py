
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


PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_PATH = PROJECT_ROOT / "data" / "transactions.csv"

# Only information potentially available before execution.
FEATURE_COLUMNS = [
    "type",
    "amount",
    "oldbalanceOrg",
    "oldbalanceDest",
]

TRAIN_END_STEP = 299
VAL_END_STEP = 354
THRESHOLD = 0.80


def evaluate(name, data, predictions):
    print(f"\n{name}")
    print("-" * 55)

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
    print("GRAPHGUARD - PREAUTHORIZATION BASELINE")
    print("-" * 55)

    df = pd.read_csv(
        DATA_PATH,
        usecols=["step", "isFraud"] + FEATURE_COLUMNS,
    )

    # Exclude held-out test transactions.
    df = df[df["step"] <= VAL_END_STEP].copy()

    train_df = df[df["step"] <= TRAIN_END_STEP]
    val_df = df[
        (df["step"] > TRAIN_END_STEP)
        & (df["step"] <= VAL_END_STEP)
    ].copy()

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

    print(f"Training sample: {len(train_sample):,}")
    print(f"Validation transactions: {len(val_df):,}")

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

    model = RandomForestClassifier(
        n_estimators=50,
        max_depth=12,
        min_samples_leaf=5,
        random_state=42,
        n_jobs=2,
    )

    print("\nTraining preauthorization Random Forest...")
    model.fit(X_train, train_sample["isFraud"])

    probabilities = model.predict_proba(X_val)[:, 1]

    predictions = pd.Series(
        (probabilities >= THRESHOLD).astype(int),
        index=val_df.index,
    )

    evaluate(
        "PREAUTHORIZATION RANDOM FOREST",
        val_df,
        predictions,
    )

    ap = average_precision_score(
        val_df["isFraud"],
        probabilities,
    )

    print(f"\nAverage precision (AP): {ap:.4f}")

    print("\nFEATURE IMPORTANCE")
    print("-" * 55)

    importance = pd.DataFrame({
        "Feature": X_train.columns,
        "Importance": model.feature_importances_,
    }).sort_values(
        "Importance",
        ascending=False,
    )

    print(importance.to_string(index=False))

    print("\nPreauthorization experiment complete!")


if __name__ == "__main__":
    main()
