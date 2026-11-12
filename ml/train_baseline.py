
from pathlib import Path

import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    confusion_matrix,
    precision_score,
    recall_score,
    f1_score,
)

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

print("GRAPHGUARD - RANDOM FOREST BASELINE")
print("-" * 45)

# Load only required columns
df = pd.read_csv(
    DATA_PATH,
    usecols=["step", "isFraud"] + FEATURE_COLUMNS,
)

# Use the chronological boundaries we verified
train_df = df[df["step"] <= 299]
val_df = df[
    (df["step"] >= 300) &
    (df["step"] <= 354)
]

# Small, reproducible first training experiment
# Keep all fraud cases and sample legitimate cases
train_fraud = train_df[train_df["isFraud"] == 1]

train_legitimate = train_df[
    train_df["isFraud"] == 0
].sample(n=100_000, random_state=42)

train_sample = pd.concat(
    [train_fraud, train_legitimate],
    ignore_index=True,
)

print("\nTraining sample:", len(train_sample))
print("Fraud cases:", len(train_fraud))

# Prepare features
X_train = pd.get_dummies(
    train_sample[FEATURE_COLUMNS],
    columns=["type"],
    dtype="int8",
)

y_train = train_sample["isFraud"]

X_val = pd.get_dummies(
    val_df[FEATURE_COLUMNS],
    columns=["type"],
    dtype="int8",
)

X_val = X_val.reindex(
    columns=X_train.columns,
    fill_value=0,
)

y_val = val_df["isFraud"]

# Train the Random Forest
model = RandomForestClassifier(
    n_estimators=50,
    max_depth=12,
    min_samples_leaf=5,
    random_state=42,
    n_jobs=2,
)

print("\nTraining Random Forest...")
model.fit(X_train, y_train)

print("Training complete!")

# Predict validation transactions
predictions = model.predict(X_val)

# Evaluate
tn, fp, fn, tp = confusion_matrix(
    y_val,
    predictions,
    labels=[0, 1],
).ravel()

print("\nVALIDATION RESULTS")
print("-" * 45)

print("True positives:", tp)
print("False positives:", fp)
print("True negatives:", tn)
print("False negatives:", fn)

print(
    f"Precision: {precision_score(y_val, predictions, zero_division=0):.4f}"
)
print(
    f"Recall: {recall_score(y_val, predictions, zero_division=0):.4f}"
)
print(
    f"F1 score: {f1_score(y_val, predictions, zero_division=0):.4f}"
)

false_positive_rate = fp / (fp + tn)

print(f"False positive rate: {false_positive_rate:.6%}")


# STEP 283 - FRAUD PROBABILITY THRESHOLD ANALYSIS

print("\nFRAUD THRESHOLD COMPARISON")
print("-" * 65)

# Probability that each transaction is fraudulent
fraud_probabilities = model.predict_proba(X_val)[:, 1]

thresholds = [0.30, 0.40, 0.50, 0.60, 0.70, 0.80, 0.90]

print(
    f"{'Threshold':<12}"
    f"{'Precision':<12}"
    f"{'Recall':<12}"
    f"{'F1':<10}"
    f"{'FP':<10}"
    f"{'FN':<10}"
)

for threshold in thresholds:

    # Classify transactions using this threshold
    threshold_predictions = (
        fraud_probabilities >= threshold
    ).astype(int)

    precision = precision_score(
        y_val, threshold_predictions, zero_division=0
    )

    recall = recall_score(
        y_val, threshold_predictions, zero_division=0
    )

    f1 = f1_score(
        y_val, threshold_predictions, zero_division=0
    )

    tn, fp, fn, tp = confusion_matrix(
        y_val,
        threshold_predictions,
        labels=[0, 1],
    ).ravel()

    print(
        f"{threshold:<12.2f}"
        f"{precision:<12.4f}"
        f"{recall:<12.4f}"
        f"{f1:<10.4f}"
        f"{fp:<10}"
        f"{fn:<10}"
    )



print("\nBaseline experiment complete!")
