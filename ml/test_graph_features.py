
import pandas as pd
import pytest

from graph_features import calculate_prior_incoming_amount


def make_df(rows):
    return pd.DataFrame(
        rows,
        columns=["step", "type", "amount", "nameOrig", "nameDest"],
    )


def test_basic_incoming_amount():
    df = make_df([
        (10, "TRANSFER", 500, "A", "B"),
        (12, "TRANSFER", 300, "C", "B"),
        (15, "TRANSFER", 700, "B", "D"),
        (16, "TRANSFER", 100, "B", "E"),
        (40, "TRANSFER", 200, "B", "F"),
    ])

    assert calculate_prior_incoming_amount(df) == [
        0.0, 0.0, 800.0, 800.0, 0.0
    ]


def test_same_step_leakage():
    df = make_df([
        (10, "TRANSFER", 500, "A", "B"),
        (10, "TRANSFER", 300, "B", "C"),
        (11, "TRANSFER", 200, "B", "D"),
    ])

    assert calculate_prior_incoming_amount(df) == [
        0.0, 0.0, 500.0
    ]


def test_repeated_incoming_transfers():
    df = make_df([
        (1, "TRANSFER", 100, "A", "B"),
        (2, "TRANSFER", 150, "A", "B"),
        (3, "CASH_OUT", 50, "B", "C"),
    ])

    assert calculate_prior_incoming_amount(df) == [
        0.0, 0.0, 250.0
    ]


def test_nonconsecutive_dataframe_indexes():
    df = make_df([
        (1, "TRANSFER", 100, "A", "B"),
        (2, "TRANSFER", 200, "C", "B"),
        (3, "TRANSFER", 50, "B", "D"),
    ])

    df.index = [10, 25, 100]

    assert calculate_prior_incoming_amount(df) == [
        0.0, 0.0, 300.0
    ]


def test_unsorted_transactions():
    df = make_df([
        (5, "TRANSFER", 100, "A", "B"),
        (3, "TRANSFER", 200, "B", "C"),
    ])

    with pytest.raises(ValueError):
        calculate_prior_incoming_amount(df)


def test_missing_required_column():
    df = make_df([
        (1, "TRANSFER", 100, "A", "B"),
    ])

    df = df.drop(columns=["amount"])

    with pytest.raises(ValueError):
        calculate_prior_incoming_amount(df)


def test_invalid_window():
    df = make_df([
        (1, "TRANSFER", 100, "A", "B"),
    ])

    with pytest.raises(ValueError):
        calculate_prior_incoming_amount(df, window=-1)


def test_exact_window_boundary():
    df = make_df([
        (1, "TRANSFER", 100, "A", "B"),
        (25, "TRANSFER", 50, "B", "C"),
        (26, "TRANSFER", 50, "B", "D"),
    ])

    assert calculate_prior_incoming_amount(df, window=24) == [
        0.0, 100.0, 0.0
    ]


def test_excluded_transaction_types():
    df = make_df([
        (1, "PAYMENT", 100, "A", "B"),
        (2, "TRANSFER", 50, "B", "C"),
    ])

    assert calculate_prior_incoming_amount(df) == [
        0.0, 0.0
    ]
