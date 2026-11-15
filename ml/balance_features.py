
import pandas as pd


def calculate_exact_depletion(transactions):
    """
    Identify CASH_OUT transactions where the withdrawal
    approximately equals the origin account's starting
    balance and the ending balance is approximately zero.

    This is a post-transaction feature.
    """

    required_columns = [
        "type",
        "amount",
        "oldbalanceOrg",
        "newbalanceOrig",
    ]

    missing = [
        column
        for column in required_columns
        if column not in transactions.columns
    ]

    if missing:
        raise ValueError(
            f"Missing required columns: {missing}"
        )

    exact_depletion = (
        (transactions["type"] == "CASH_OUT")
        & (transactions["oldbalanceOrg"] > 0)
        & (
            (
                transactions["oldbalanceOrg"]
                - transactions["amount"]
            ).abs() <= 0.01
        )
        & (transactions["newbalanceOrig"].abs() <= 0.01)
    )

    return exact_depletion.astype("int8")


def calculate_full_balance_attempt(transactions):
    """
    Identify transactions that attempt to move the
    sender's entire starting balance.

    Uses only transaction type, amount, and the
    sender's pre-transaction balance.

    Note: This is a PaySim benchmark feature.
    """

    required_columns = [
        "type",
        "amount",
        "oldbalanceOrg",
    ]

    missing = [
        column
        for column in required_columns
        if column not in transactions.columns
    ]

    if missing:
        raise ValueError(
            f"Missing required columns: {missing}"
        )

    full_balance_attempt = (
        transactions["type"].isin(
            ["TRANSFER", "CASH_OUT"]
        )
        & (transactions["oldbalanceOrg"] > 0)
        & (
            (
                transactions["amount"]
                - transactions["oldbalanceOrg"]
            ).abs() <= 0.01
        )
    )

    return full_balance_attempt.astype("int8")
