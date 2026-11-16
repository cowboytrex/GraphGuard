from sklearn.metrics import confusion_matrix, precision_score, recall_score, f1_score

from preauth_baseline import train_and_score_validation

SEGMENTS = [
    (300, 310),
    (311, 321),
    (322, 332),
    (333, 343),
    (344, 354),
]


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
    print("\nTraining preauthorization baseline...")

    validation = train_and_score_validation()

    print(f"Validation transactions: {len(validation):,}")

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