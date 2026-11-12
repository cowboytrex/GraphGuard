
from collections import defaultdict, deque


def calculate_prior_unique_senders(transactions, window=24):
    """
    Count distinct historical senders to each receiver.

    Transactions must be sorted by step.
    Transactions within the same step are processed
    together to avoid using same-step information.
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
    results = [0] * len(transactions)

    transfer_types = {"TRANSFER", "CASH_OUT"}

    for step, group in transactions.groupby("step", sort=False):
        # Calculate features using only previous steps.
        for position in group.index:
            receiver = transactions.at[position, "nameDest"]
            recent = history[receiver]

            while recent and recent[0][0] < step - window:
                recent.popleft()

            results[position] = len({
                sender for _, sender in recent
            })

        # Update history after every transaction in
        # this step has received its feature value.
        for position in group.index:
            if transactions.at[position, "type"] in transfer_types:
                receiver = transactions.at[position, "nameDest"]
                sender = transactions.at[position, "nameOrig"]
                history[receiver].append((step, sender))

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
