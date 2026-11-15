
from collections import Counter, defaultdict, deque


def calculate_prior_unique_senders(transactions, window=24):
    """
    Count distinct historical senders to each receiver.

    Uses a sliding window and sender frequency counters.
    Only transactions from earlier steps contribute.
    """
    required = {"step", "type", "nameOrig", "nameDest"}
    missing = required - set(transactions.columns)

    if missing:
        raise ValueError(f"Missing columns: {sorted(missing)}")

    if window <= 0:
        raise ValueError("window must be positive")

    if not transactions["step"].is_monotonic_increasing:
        raise ValueError("Transactions must be sorted by step")

    history = defaultdict(deque)
    sender_counts = defaultdict(Counter)

    results = [0] * len(transactions)
    transfer_types = {"TRANSFER", "CASH_OUT"}

    rows = transactions[
        ["step", "type", "nameOrig", "nameDest"]
    ].itertuples(index=False, name=None)

    current_step = None
    step_rows = []

    def process_step(step, batch):
        # Calculate features before adding this step's transactions.
        for position, tx_type, sender, receiver in batch:
            recent = history[receiver]
            counts = sender_counts[receiver]

            while recent and recent[0][0] < step - window:
                _, old_sender = recent.popleft()
                counts[old_sender] -= 1

                if counts[old_sender] == 0:
                    del counts[old_sender]

            results[position] = len(counts)

        # Update history only after calculating all features.
        for position, tx_type, sender, receiver in batch:
            if tx_type in transfer_types:
                history[receiver].append((step, sender))
                sender_counts[receiver][sender] += 1

    for position, (step, tx_type, sender, receiver) in enumerate(rows):
        if current_step is None:
            current_step = step

        if step != current_step:
            process_step(current_step, step_rows)
            step_rows = []
            current_step = step

        step_rows.append((position, tx_type, sender, receiver))

    if step_rows:
        process_step(current_step, step_rows)

    return results



if __name__ == "__main__":
    import pandas as pd

    sample = pd.DataFrame([
        {"step": 1, "type": "TRANSFER",
         "nameOrig": "A", "nameDest": "X"},
        {"step": 2, "type": "TRANSFER",
         "nameOrig": "B", "nameDest": "X"},
        {"step": 3, "type": "TRANSFER",
         "nameOrig": "A", "nameDest": "X"},
        {"step": 4, "type": "CASH_OUT",
         "nameOrig": "C", "nameDest": "X"},
        {"step": 5, "type": "TRANSFER",
         "nameOrig": "D", "nameDest": "X"},
    ])

    result = calculate_prior_unique_senders(sample)

    expected = [0, 1, 2, 2, 3]

    print("Calculated:", result)
    print("Expected:  ", expected)

    assert result == expected
    print("Graph feature self-check PASSED!")


    # Test 2: Same-step transactions must not see each other.
    same_step = pd.DataFrame([
        {"step": 1, "type": "TRANSFER",
         "nameOrig": "A", "nameDest": "X"},
        {"step": 1, "type": "TRANSFER",
         "nameOrig": "B", "nameDest": "X"},
        {"step": 2, "type": "TRANSFER",
         "nameOrig": "C", "nameDest": "X"},
    ])

    same_step_result = calculate_prior_unique_senders(same_step)

    assert same_step_result == [0, 0, 2]
    print("Same-step leakage test PASSED!")

    # Test 3: Old transactions must expire.
    expiration = pd.DataFrame([
        {"step": 1, "type": "TRANSFER",
         "nameOrig": "A", "nameDest": "X"},
        {"step": 2, "type": "TRANSFER",
         "nameOrig": "B", "nameDest": "X"},
        {"step": 27, "type": "TRANSFER",
         "nameOrig": "C", "nameDest": "X"},
    ])

    expiration_result = calculate_prior_unique_senders(expiration)

    assert expiration_result == [0, 1, 0]
    print("24-step window expiration test PASSED!")


    # Test 4: Nonconsecutive DataFrame indices.
    nonconsecutive = pd.DataFrame([
        {"step": 1, "type": "TRANSFER",
         "nameOrig": "A", "nameDest": "X"},
        {"step": 2, "type": "TRANSFER",
         "nameOrig": "B", "nameDest": "X"},
        {"step": 3, "type": "TRANSFER",
         "nameOrig": "C", "nameDest": "X"},
    ])

    nonconsecutive.index = [10, 25, 100]

    nonconsecutive_result = calculate_prior_unique_senders(
        nonconsecutive
    )

    assert nonconsecutive_result == [0, 1, 2]
    print("Nonconsecutive index test PASSED!")
