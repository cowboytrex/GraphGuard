
import time
import pandas as pd

from graph_features import calculate_prior_unique_senders


DATA_PATH = "data/transactions.csv"

SAMPLE_SIZES = [10_000, 100_000, 500_000]


def run_benchmark():
    print("GRAPHGUARD - GRAPH FEATURE BENCHMARK")
    print("-" * 50)

    for size in SAMPLE_SIZES:
        print(f"\nTesting {size:,} transactions...")

        start_load = time.perf_counter()

        df = pd.read_csv(
            DATA_PATH,
            usecols=["step", "type", "nameOrig", "nameDest"],
            nrows=size,
        )

        load_time = time.perf_counter() - start_load

        start_compute = time.perf_counter()

        features = calculate_prior_unique_senders(df)

        compute_time = time.perf_counter() - start_compute

        print(f"Load time: {load_time:.3f} seconds")
        print(f"Feature computation: {compute_time:.3f} seconds")
        print(f"Features generated: {len(features):,}")
        print(f"Maximum unique senders: {max(features)}")

        assert len(features) == len(df)

    print("\nAll benchmark checks PASSED!")


if __name__ == "__main__":
    run_benchmark()
