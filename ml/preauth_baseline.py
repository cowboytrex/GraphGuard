from pathlib import Path

import pandas as pd
from sklearn.ensemble import RandomForestClassifier

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_PATH = PROJECT_ROOT / "data" / "transactions.csv"

# Only information available before the transaction is authorized.
FEATURES = ["type", "amount", "oldbalanceOrg", "oldbalanceDest"]

TRAIN_END_STEP = 299
VAL_END_STEP = 354  # Steps after this are the held-out test period.
LEGITIMATE_SAMPLE_SIZE = 100_000
RANDOM_STATE = 42
THRESHOLD = 0.80


def encode_features(data, reference_columns=None):
    X = pd.get_dummies(data[FEATURES], columns=["type"], dtype="int8")
    if reference_columns is not None:
        X = X.reindex(columns=reference_columns, fill_value=0)
    return X


def train_and_score_validation():
    """Train the preauthorization baseline and score the validation period.

    Returns the validation rows with 'probability' and 'prediction' columns.
    """
    df = pd.read_csv(DATA_PATH, usecols=["step", "isFraud"] + FEATURES)

    # Drop the held-out test period before anything else touches the data.
    df = df[df["step"] <= VAL_END_STEP].copy()

    train = df[df["step"] <= TRAIN_END_STEP]
    validation = df[df["step"] > TRAIN_END_STEP].copy()

    fraud = train[train["isFraud"] == 1]
    legitimate = train[train["isFraud"] == 0].sample(
        n=LEGITIMATE_SAMPLE_SIZE, random_state=RANDOM_STATE
    )
    training_sample = pd.concat([fraud, legitimate], ignore_index=True)

    X_train = encode_features(training_sample)
    X_val = encode_features(validation, reference_columns=X_train.columns)

    model = RandomForestClassifier(
        n_estimators=50,
        max_depth=12,
        min_samples_leaf=5,
        random_state=RANDOM_STATE,
        n_jobs=2,
    )
    model.fit(X_train, training_sample["isFraud"])

    validation["probability"] = model.predict_proba(X_val)[:, 1]
    validation["prediction"] = (
        validation["probability"] >= THRESHOLD
    ).astype(int)

    return validation