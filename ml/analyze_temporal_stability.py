
from pathlib import Path

import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import confusion_matrix, precision_score, recall_score, f1_score

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_PATH = PROJECT_ROOT / "data" / "transactions.csv"

FEATURES = ["type", "amount", "oldbalanceOrg", "oldbalanceDest"]
THRESHOLD = 0.80

SEGMENTS = [
    (300, 310),
    (311, 321),
    (322, 332),
    (333, 343),
    (344, 354),
]


def prepare_features(data, reference_columns=None):
    X = pd.get_dummies(
        data[FEATURES],
        columns=["type"],
        dtype="int8",
    )

    if reference_columns is not None:
        X = X.reindex(columns=reference_columns, fill_value=0)

    return X


def evaluate(name, data):
    y_true = data["isFraud"]
    y_pred = data["prediction"]

    tn, fp, fn, tp = confusion_matrix(
        y_true, y_pred, labels=[0, 1]
    ).ravel()

    print(f"\n{name}")
    print("-" * 55)
    print(f"Transactions: {len(data):,}")
    print(f"Actual fraud: {int(y_true.sum()):,}")
    print(f"TP: {tp:,} | FP: {fp:,}")
    print(f"TN: {tn:,} | FN: {fn:,}")
    print(
        f"Precision: {precision_score(y_true, y_pred, zero_division=0):.4f}"
    )
    print(
        f"Recall: {recall_score(y_true, y_pred, zero_division=0):.4f}"
    )
    print(
        f"F1: {f1_score(y_true, y_pred, zero_division=0):.4f}"
    )


def main():
    print("GRAPHGUARD - TEMPORAL STABILITY ANALYSIS")
    print("-" * 60)

    columns = ["step", "isFraud"] + FEATURES
    df = pd.read_csv(DATA_PATH, usecols=columns)

    # Training and validation only; exclude held-out test.
    df = df[df["step"] <= 354].copy()

    train = df[df["step"] <= 299]
    validation = df[
        (df["step"] >= 300) & (df["step"] <= 354)
    ].copy()

    fraud = train[train["isFraud"] == 1]
    legitimate = train[train["isFraud"] == 0].sample(
        n=100_000, random_state=42
    )

    training_sample = pd.concat(
        [fraud, legitimate], ignore_index=True
    )

    X_train = prepare_features(training_sample)
    X_val = prepare_features(
        validation, reference_columns=X_train.columns
    )

    model = RandomForestClassifier(
        n_estimators=50,
        max_depth=12,
        min_samples_leaf=5,
        random_state=42,
        n_jobs=2,
    )

    print(f"Training transactions: {len(training_sample):,}")
    print(f"Validation transactions: {len(validation):,}")
    print("\nTraining preauthorization baseline...")

    model.fit(X_train, training_sample["isFraud"])

    probabilities = model.predict_proba(X_val)[:, 1]
    validation["prediction"] = (
        probabilities >= THRESHOLD
    ).astype(int)

    evaluate("FULL VALIDATION PERIOD", validation)

    for start, end in SEGMENTS:
        segment = validation[
            (validation["step"] >= start)
            & (validation["step"] <= end)
        ]

        evaluate(f"STEPS {start}-{end}", segment)

    print("\nTemporal stability analysis complete!")


if __name__ == "__main__":
    main()
