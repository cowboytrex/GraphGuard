
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
