import pandas as pd
from sklearn.metrics import roc_auc_score

from preauth_baseline import train_and_score_validation
from analyze_temporal_stability import SEGMENTS


def add_columns(validation):
    v = validation
    # PaySim fraud only occurs in TRANSFER and CASH_OUT; group the rest.
    v["type_group"] = v["type"].where(
        v["type"].isin(["TRANSFER", "CASH_OUT"]), "OTHER"
    )
    v["is_tp"] = (v["prediction"] == 1) & (v["isFraud"] == 1)
    v["is_fp"] = (v["prediction"] == 1) & (v["isFraud"] == 0)

    # Bins (299, 310], (310, 321], ... built from SEGMENTS so the
    # boundaries can never drift from the temporal stability script.
    bins = [SEGMENTS[0][0] - 1] + [end for _, end in SEGMENTS]
    labels = [f"{start}-{end}" for start, end in SEGMENTS]
    v["segment"] = pd.cut(v["step"], bins=bins, labels=labels)
    return v


def volume_per_step(v):
    print("\n1. TRANSACTIONS PER STEP (tests H1)")
    print("-" * 60)
    per_step = v.groupby(["segment", "step"], observed=True).size()
    summary = per_step.groupby(level="segment", observed=True).agg(
        ["min", "median", "max"]
    )
    print(summary.to_string())


def composition_by_type(v):
    print("\n2. COMPOSITION AND FALSE POSITIVES BY TYPE (tests H3)")
    print("-" * 60)
    table = v.groupby(["segment", "type_group"], observed=True).agg(
        rows=("isFraud", "size"),
        fraud=("isFraud", "sum"),
        tp=("is_tp", "sum"),
        fp=("is_fp", "sum"),
    )
    table["legit"] = table["rows"] - table["fraud"]
    table["fp_per_10k_legit"] = (
        1e4 * table["fp"] / table["legit"]
    ).round(2)
    print(table[["legit", "fraud", "tp", "fp", "fp_per_10k_legit"]].to_string())


def precision_vs_prevalence(v):
    print("\n3. OBSERVED VS EXPECTED PRECISION (tests H2)")
    print("-" * 60)
    legit_total = (v["isFraud"] == 0).sum()
    overall_fpr = v["is_fp"].sum() / legit_total
    print(f"Overall FP rate: {1e4 * overall_fpr:.2f} per 10k legitimate\n")

    rows = []
    for label, seg in v.groupby("segment", observed=True):
        tp = seg["is_tp"].sum()
        fp = seg["is_fp"].sum()
        fraud = seg["isFraud"].sum()
        legit = len(seg) - fraud
        expected_fp = legit * overall_fpr
        rows.append({
            "segment": label,
            "fraud_rate_%": round(100 * fraud / len(seg), 3),
            "fp_per_10k": round(1e4 * fp / legit, 2),
            "precision": round(tp / (tp + fp), 4),
            "expected_precision": round(tp / (tp + expected_fp), 4),
            "roc_auc": round(roc_auc_score(seg["isFraud"], seg["probability"]), 4),
        })
    print(pd.DataFrame(rows).to_string(index=False))


def main():
    print("GRAPHGUARD - SEGMENT COMPOSITION DIAGNOSTIC")
    print("-" * 60)
    validation = add_columns(train_and_score_validation())
    volume_per_step(validation)
    composition_by_type(validation)
    precision_vs_prevalence(validation)
    print("\nSegment composition diagnostic complete!")


if __name__ == "__main__":
    main()