
from pathlib import Path
import pandas as pd

# Find the project root
PROJECT_ROOT = Path(__file__).resolve().parent.parent

# Locate the existing PaySim dataset
DATA_PATH = PROJECT_ROOT / "data" / "transactions.csv"

# Load the CSV
df = pd.read_csv(DATA_PATH)

print("GRAPHGUARD - DATASET EXPLORATION")
print("-" * 45)

# Dataset dimensions
print("\nDataset shape:")
print(df.shape)

# Column names
print("\nColumns:")
print(df.columns.tolist())

# Preview the data
print("\nFirst 5 transactions:")
print(df.head())

# Check missing values
print("\nMissing values:")
print(df.isnull().sum())

# Count transaction types
print("\nTransaction types:")
print(df["type"].value_counts())

# Count fraudulent and legitimate transactions
print("\nFraud distribution:")
print(df["isFraud"].value_counts())

# Calculate fraud percentage
fraud_rate = df["isFraud"].mean() * 100

print(f"\nFraud rate: {fraud_rate:.4f}%")


# Analyze fraud by transaction type
print("\nFraud by transaction type:")
fraud_by_type = pd.crosstab(df["type"], df["isFraud"])
print(fraud_by_type)

# Calculate fraud rate for each transaction type
print("\nFraud rate by transaction type (%):")
fraud_rate_by_type = (
    df.groupby("type")["isFraud"].mean() * 100
)
print(fraud_rate_by_type.round(4))

# Examine fraud distribution over time
print("\nTransaction step range:")
print("Minimum step:", df["step"].min())
print("Maximum step:", df["step"].max())
