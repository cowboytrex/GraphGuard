
import time
import tracemalloc

import pandas as pd

from graph_features import calculate_prior_incoming_amount


DATA_PATH = "data/transactions.csv"

# Begin with smaller samples before benchmarking all transactions.
SAMPLE_SIZES = [10_000, 100_000, 500_000]


def run_benchmark():
    print("GRAPHGUARD - INCOMING FUNDS FEATURE BENCHMARK")
    print("-" * 55)

    for size in SAMPLE_SIZES:
        print(f"\nTesting {size:,} transactions...")

        load_start = time.perf_counter()

        df = pd.read_csv(
            DATA_PATH,
            usecols=[
                "step",
                "type",
                "amount",
                "nameOrig",
                "nameDest",
            ],
            nrows=size,
        )

        load_time = time.perf_counter() - load_start

        tracemalloc.start()
        feature_start = time.perf_counter()

        features = calculate_prior_incoming_amount(
            df,
            window=24,
        )

        feature_time = time.perf_counter() - feature_start

        _, peak_memory = tracemalloc.get_traced_memory()
        tracemalloc.stop()

        assert len(features) == len(df)
        assert all(value >= 0 for value in features)

        nonzero_count = sum(value > 0 for value in features)

        print(f"Load time: {load_time:.3f} seconds")
        print(f"Feature computation: {feature_time:.3f} seconds")
        print(
            f"Peak traced memory: "
            f"{peak_memory / 1_000_000:.2f} MB"
        )
        print(f"Features generated: {len(features):,}")
        print(f"Nonzero feature values: {nonzero_count:,}")
        print(f"Maximum incoming amount: {max(features):,.2f}")

    print("\nAll benchmark checks PASSED!")


if __name__ == "__main__":
    run_benchmark()
