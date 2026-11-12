
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
split_step = 595

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
