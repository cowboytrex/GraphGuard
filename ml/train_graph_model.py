
from pathlib import Path

import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    confusion_matrix,
    precision_score,
    recall_score,
    f1_score,
    precision_recall_curve,
    average_precision_score,
)

from graph_features import calculate_prior_unique_senders


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

GRAPH_FEATURE = "prior_unique_senders_24h"

TRAIN_END_STEP = 299
VAL_END_STEP = 354
THRESHOLD = 0.80


def prepare_features(subset, feature_columns, reference_columns=None):
    X = pd.get_dummies(
        subset[feature_columns],
        columns=["type"],
        dtype="int8",
    )

    if reference_columns is not None:
        X = X.reindex(
            columns=reference_columns,
            fill_value=0,
        )

    return X


def evaluate_model(name, model, X_val, y_val):
    probabilities = model.predict_proba(X_val)[:, 1]
    predictions = (probabilities >= THRESHOLD).astype(int)

    tn, fp, fn, tp = confusion_matrix(
        y_val,
        predictions,
        labels=[0, 1],
    ).ravel()

    precision = precision_score(
        y_val, predictions, zero_division=0
    )
    recall = recall_score(
        y_val, predictions, zero_division=0
    )
    f1 = f1_score(
        y_val, predictions, zero_division=0
    )

    print(f"\n{name}")
    print("-" * 45)
    print(f"Threshold: {THRESHOLD:.2f}")
    print(f"True positives: {tp:,}")
    print(f"False positives: {fp:,}")
    print(f"True negatives: {tn:,}")
    print(f"False negatives: {fn:,}")
    print(f"Precision: {precision:.4f}")
    print(f"Recall: {recall:.4f}")
    print(f"F1 score: {f1:.4f}")
    print(f"False positive rate: {fp / (fp + tn):.6%}")

    return {
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "fp": fp,
        "fn": fn,
    }


def compare_thresholds(name, model, X_val, y_val):
    probabilities = model.predict_proba(X_val)[:, 1]

    print(f"\nTHRESHOLD ANALYSIS: {name}")
    print("-" * 75)

    print(
        f"{'Threshold':<12}"
        f"{'Precision':<12}"
        f"{'Recall':<12}"
        f"{'F1':<12}"
        f"{'FP':<10}"
        f"{'FN':<10}"
    )

    thresholds = [
        0.30, 0.40, 0.50, 0.60,
        0.65, 0.70, 0.75, 0.80,
        0.85, 0.90, 0.95,
    ]

    best_f1 = -1.0
    best_threshold = None

    for threshold in thresholds:
        predictions = (probabilities >= threshold).astype(int)

        precision = precision_score(
            y_val, predictions, zero_division=0
        )
        recall = recall_score(
            y_val, predictions, zero_division=0
        )
        f1 = f1_score(
            y_val, predictions, zero_division=0
        )

        tn, fp, fn, tp = confusion_matrix(
            y_val, predictions, labels=[0, 1]
        ).ravel()

        print(
            f"{threshold:<12.2f}"
            f"{precision:<12.4f}"
            f"{recall:<12.4f}"
            f"{f1:<12.4f}"
            f"{fp:<10}"
            f"{fn:<10}"
        )

        if f1 > best_f1:
            best_f1 = f1
            best_threshold = threshold

    average_precision = average_precision_score(
        y_val, probabilities
    )

    # Calculate the full precision-recall curve.
    precision_curve, recall_curve, curve_thresholds = (
        precision_recall_curve(y_val, probabilities)
    )

    print(f"\nBest tested threshold: {best_threshold:.2f}")
    print(f"Best tested F1: {best_f1:.4f}")
    print(f"Average precision (AP): {average_precision:.4f}")
    print(
        "Precision-recall curve points:",
        len(precision_curve),
    )

    return {
        "best_threshold": best_threshold,
        "best_f1": best_f1,
        "average_precision": average_precision,
    }


def main():
    print("GRAPHGUARD - BASELINE VS GRAPH-ENHANCED MODEL")
    print("-" * 55)

    columns = [
        "step",
        "isFraud",
        "nameOrig",
        "nameDest",
    ] + BASE_FEATURES

    df = pd.read_csv(
        DATA_PATH,
        usecols=columns,
    )

    # Do not use the held-out test period.
    df = df[df["step"] <= VAL_END_STEP].copy()

    df = df.sort_values(
        "step",
        kind="stable",
    ).reset_index(drop=True)

    print(f"Development transactions: {len(df):,}")

    print("\nCalculating historical graph features...")

    df[GRAPH_FEATURE] = calculate_prior_unique_senders(
        df,
        window=24,
    )

    train_df = df[df["step"] <= TRAIN_END_STEP]
    val_df = df[
        (df["step"] > TRAIN_END_STEP)
        & (df["step"] <= VAL_END_STEP)
    ]

    print(f"Training transactions: {len(train_df):,}")
    print(f"Validation transactions: {len(val_df):,}")

    # Reproduce the original baseline sampling strategy.
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

    y_train = train_sample["isFraud"]
    y_val = val_df["isFraud"]

    experiments = [
        ("Baseline Random Forest", BASE_FEATURES),
        (
            "Graph-Enhanced Random Forest",
            BASE_FEATURES + [GRAPH_FEATURE],
        ),
    ]

    results = {}

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

        model.fit(X_train, y_train)

        results[name] = evaluate_model(
            name,
            model,
            X_val,
            y_val,
        )

        compare_thresholds(
            name,
            model,
            X_val,
            y_val,
        )

        if GRAPH_FEATURE in X_train.columns:
            feature_index = X_train.columns.get_loc(
                GRAPH_FEATURE
            )

            print(
                "Graph feature importance:",
                f"{model.feature_importances_[feature_index]:.6f}",
            )

    baseline = results["Baseline Random Forest"]
    graph = results["Graph-Enhanced Random Forest"]

    print("\nMODEL COMPARISON")
    print("-" * 55)

    print(
        f"F1 difference: "
        f"{graph['f1'] - baseline['f1']:+.4f}"
    )
    print(
        f"Precision difference: "
        f"{graph['precision'] - baseline['precision']:+.4f}"
    )
    print(
        f"Recall difference: "
        f"{graph['recall'] - baseline['recall']:+.4f}"
    )
    print(
        f"False positive difference: "
        f"{graph['fp'] - baseline['fp']:+,}"
    )
    print(
        f"False negative difference: "
        f"{graph['fn'] - baseline['fn']:+,}"
    )

    print("\nGraph-enhanced model experiment complete!")


if __name__ == "__main__":
    main()
