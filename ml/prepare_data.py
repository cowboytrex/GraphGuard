
from pathlib import Path
import pandas as pd

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_PATH = PROJECT_ROOT / "data" / "transactions.csv"

print("GRAPHGUARD - DATA PREPARATION")
print("-" * 45)

# Load only the columns needed for the baseline
columns = [
    "step",
    "type",
    "amount",
    "oldbalanceOrg",
    "newbalanceOrig",
    "oldbalanceDest",
    "newbalanceDest",
    "isFraud",
]

df = pd.read_csv(DATA_PATH, usecols=columns)

# Split by transaction step

# Find a chronological split near 80% of transactions
transactions_per_step = df.groupby("step").size()

cumulative_transactions = transactions_per_step.cumsum()

target_train_size = len(df) * 0.80

split_step = (
    cumulative_transactions
    .sub(target_train_size)
    .abs()
    .idxmin()
)

print("\nCalculated split step:", split_step)

train_df = df[df["step"] <= split_step]
test_df = df[df["step"] > split_step]

print("\nTraining transactions:", len(train_df))
print("Testing transactions:", len(test_df))

print("\nTraining fraud distribution:")
print(train_df["isFraud"].value_counts())

print("\nTesting fraud distribution:")
print(test_df["isFraud"].value_counts())

print("\nTraining fraud rate:")
print(f"{train_df['isFraud'].mean() * 100:.4f}%")

print("\nTesting fraud rate:")
print(f"{test_df['isFraud'].mean() * 100:.4f}%")

# Confirm the time periods do not overlap
assert train_df["step"].max() < test_df["step"].min()

print("\nChronological split verified!")


# Select transaction-level features
feature_columns = [
    "type",
    "amount",
    "oldbalanceOrg",
    "newbalanceOrig",
    "oldbalanceDest",
    "newbalanceDest",
]

# Separate features (X) from labels (y)
X_train = train_df[feature_columns].copy()
X_test = test_df[feature_columns].copy()

y_train = train_df["isFraud"]
y_test = test_df["isFraud"]

# Convert transaction categories into numeric columns
X_train = pd.get_dummies(X_train, columns=["type"], dtype=int)
X_test = pd.get_dummies(X_test, columns=["type"], dtype=int)

# Ensure both datasets have identical feature columns
X_test = X_test.reindex(columns=X_train.columns, fill_value=0)

print("\nML feature columns:")
print(X_train.columns.tolist())

print("\nTraining feature shape:", X_train.shape)
print("Testing feature shape:", X_test.shape)

print("\nTraining labels:", y_train.shape)
print("Testing labels:", y_test.shape)

# Check for missing values
assert not X_train.isnull().any().any()
assert not X_test.isnull().any().any()

print("\nML features prepared successfully!")


# Create a validation period within the existing training data
# Target: approximately 80% of pre-test transactions
train_step_counts = train_df.groupby("step").size()
train_cumulative = train_step_counts.cumsum()

validation_boundary = (
    train_cumulative
    .sub(len(train_df) * 0.80)
    .abs()
    .idxmin()
)

# Split the pre-test data chronologically
final_train_df = train_df[train_df["step"] <= validation_boundary]
validation_df = train_df[train_df["step"] > validation_boundary]

print("\nTHREE-WAY CHRONOLOGICAL SPLIT")
print("-" * 45)

for name, subset in [
    ("Training", final_train_df),
    ("Validation", validation_df),
    ("Testing", test_df),
]:
    print(f"\n{name}:")
    print("Transactions:", len(subset))
    print("Fraud cases:", subset["isFraud"].sum())
    print(f"Fraud rate: {subset['isFraud'].mean() * 100:.4f}%")
    print("Step range:", subset["step"].min(), "-", subset["step"].max())

assert final_train_df["step"].max() < validation_df["step"].min()
assert validation_df["step"].max() < test_df["step"].min()

print("\nThree-way chronological split verified!")


# Prepare features separately for each chronological dataset

def prepare_features(subset, feature_columns):
    X = subset[feature_columns].copy()

    # Convert transaction types to numeric features
    X = pd.get_dummies(X, columns=["type"], dtype="int8")

    y = subset["isFraud"].copy()

    return X, y


# Use the same transaction-level features
feature_columns = [
    "type",
    "amount",
    "oldbalanceOrg",
    "newbalanceOrig",
    "oldbalanceDest",
    "newbalanceDest",
]

X_train, y_train = prepare_features(
    final_train_df, feature_columns
)

X_val, y_val = prepare_features(
    validation_df, feature_columns
)

X_test, y_test = prepare_features(
    test_df, feature_columns
)

# Align feature columns to the training dataset
X_val = X_val.reindex(columns=X_train.columns, fill_value=0)
X_test = X_test.reindex(columns=X_train.columns, fill_value=0)

assert list(X_train.columns) == list(X_val.columns)
assert list(X_train.columns) == list(X_test.columns)

print("\nFINAL ML DATASETS")
print("-" * 45)
print("Training:", X_train.shape)
print("Validation:", X_val.shape)
print("Testing:", X_test.shape)

print("\nFinal ML feature preparation complete!")
