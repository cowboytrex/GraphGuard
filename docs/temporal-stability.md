# GraphGuard — Validation Temporal Stability (Step 314)

## Question
Does the preauthorization baseline perform consistently across the
validation period, or does precision vary because the model degrades?

## Protocol
- Model: preauthorization Random Forest baseline (`ml/preauth_baseline.py`)
- Features: type, amount, oldbalanceOrg, oldbalanceDest
- Training: steps 1–299 (all fraud + 100,000 sampled legitimate)
- Evaluation: validation steps 300–354, split into five 11-step segments
- Threshold: 0.80 (unchanged)
- Held-out test period (steps 355+) not used
- Scripts: `ml/analyze_temporal_stability.py`,
  `ml/analyze_segment_composition.py`, `ml/analyze_cashout_misses.py`

## Results

| Steps | Fraud rate | FP per 10k legit | Precision | Expected precision* | Recall |
|---|---:|---:|---:|---:|---:|
| 300–310 | 0.031% | 2.26 | 0.513 | 0.557 | 0.775 |
| 311–321 | 0.372% | 1.84 | 0.935 | 0.933 | 0.713 |
| 322–332 | 0.034% | 1.66 | 0.566 | 0.532 | 0.629 |
| 333–343 | 0.255% | 2.45 | 0.872 | 0.898 | 0.654 |
| 344–354 | 0.042% | 1.69 | 0.636 | 0.609 | 0.712 |

*Precision expected if every segment had the overall false-positive
rate (1.90 per 10,000 legitimate transactions).

| Type | Fraud caught (validation) | Recall | Share of false positives |
|---|---:|---:|---:|
| TRANSFER | 278 / 281 | 98.9% | 91% (177 / 195) |
| CASH_OUT | 113 / 281 | 40.2% | 9% (18 / 195) |

## Observations
- Precision variation is explained by transaction volume. Segments
  311–321 and 333–343 consist largely of near-empty simulated night
  steps (median 20 and 320 transactions per step versus ~30,000),
  so the same false-positive rate produces far fewer false alarms.
- The false-positive rate is roughly constant across segments, and
  expected precision is within about 0.04 of observed precision.
  There is no evidence of model degradation over this period.
- Overall recall hides a large difference by type. All but 3 of the
  171 missed frauds are CASH_OUT.
- Every validation CASH_OUT fraud, caught or missed, moves exactly the
  sender's full balance; no legitimate CASH_OUT does. Missed frauds
  have typical amounts (median ~166k vs ~1.87M for caught fraud). The
  model appears to rely on large amounts rather than the
  amount–balance relationship, which axis-aligned tree splits
  represent poorly.

## Limitations
- Five segments of ~110 fraud cases each; segment-level recall has a
  95% interval of roughly ±0.08.
- ROC AUC (~0.998 per segment) is inflated by transaction types that
  never contain fraud and should not be quoted without that caveat.
- The validation period has been used for many experiments; these
  findings are exploratory, not independent evidence.
- PaySim fraud is strongly tied to full-balance draining, a likely
  simulator artifact. Results do not establish real-world performance.

## Decision
Precision variation is not treated as concept drift. Next work focuses
on engineering quality before the evaluation protocol is frozen.