# GraphGuard — Chronological Evaluation Baseline

## Dataset
- Source: PaySim synthetic financial transactions
- Total dataset: 6,362,620 transactions
- Total fraud cases: 8,213

## Evaluation Protocol
- Development period: steps 1–500
- Held-out evaluation period: steps 501–743
- Evaluated transactions: 300,813
- Detection method: rule-based multi-factor risk scoring
- Fraud prediction: HIGH RISK classification

## Results

| Metric | Result |
|---|---:|
| True positives | 2,572 |
| False positives | 0 |
| True negatives | 298,161 |
| False negatives | 80 |
| Accuracy | 99.97% |
| Precision | 100.00% |
| Recall | 96.98% |
| F1 score | 0.9847 |
| False-positive rate | 0.00% |
  
## Observations

The detector missed 80 fraudulent transactions. Examined examples included large transfers that reduced account balances without fully draining them.

## Limitations

- PaySim contains simulated rather than real financial transactions.
- Risk-scoring rules were developed using earlier samples of the same dataset.
- The chronological split was selected after examining aggregate dataset statistics.
- No additional tuning should be performed against this held-out period.
- Results do not establish real-world fraud detection performance.

## Future Work

- Implement account-relationship graph analysis.
- Evaluate graph-based features using a separately defined validation protocol.
- Compare graph-based detection against this rule-based baseline.