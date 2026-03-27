#!/usr/bin/env python3
"""Train fraud and rule models from transaction CSV data.

Usage:
    python train_model.py --input ml/data/autotrain_transactions.csv --models-dir ml/models
"""

from __future__ import annotations

import argparse
import json
import pickle
import re
from pathlib import Path

import pandas as pd
from sklearn.ensemble import RandomForestClassifier


RULE_COLS = [f"r{i:02d}" for i in range(1, 15)]


def to_num(series: pd.Series, default: float = 0.0) -> pd.Series:
    return pd.to_numeric(series, errors="coerce").fillna(default)


def to_int_flag(series: pd.Series) -> pd.Series:
    return to_num(series, 0).astype(int).clip(lower=0, upper=1)


def encode_series(series: pd.Series) -> tuple[pd.Series, dict[str, int]]:
    normalized = series.fillna("unknown").astype(str).str.strip()
    normalized = normalized.where(normalized != "", "unknown")
    values = sorted(normalized.unique().tolist())
    mapping = {value: idx for idx, value in enumerate(values)}
    encoded = normalized.map(mapping).astype(int)
    return encoded, mapping


def rule_flags(reason: str) -> dict[str, int]:
    flags = {col: 0 for col in RULE_COLS}
    if not reason or str(reason).strip().lower() == "none":
        return flags

    parts = [chunk.strip().upper() for chunk in str(reason).split("|") if chunk.strip()]
    for part in parts:
        match = re.match(r"R(\d{2})", part)
        if not match:
            continue
        idx = int(match.group(1))
        if 1 <= idx <= 14:
            flags[f"r{idx:02d}"] = 1
    return flags


def atomic_pickle(obj, target: Path) -> None:
    tmp = target.with_suffix(target.suffix + ".tmp")
    with tmp.open("wb") as fp:
        pickle.dump(obj, fp)
    tmp.replace(target)


def atomic_json(obj, target: Path) -> None:
    tmp = target.with_suffix(target.suffix + ".tmp")
    with tmp.open("w", encoding="utf-8") as fp:
        json.dump(obj, fp, indent=2)
    tmp.replace(target)


def build_feature_frame(df: pd.DataFrame):
    working = df.copy()

    working["amount"] = to_num(working.get("amount", 0), 0.0)
    working["balance"] = to_num(working.get("balance", 0), 0.0)
    working["txn_count_last_1hr"] = to_num(
        working.get("txn_count_last_1hr", 0), 0
    ).astype(int)
    working["txn_count_last_24hr"] = to_num(
        working.get("txn_count_last_24hr", 0), 0
    ).astype(int)
    working["avg_txn_amount_30days"] = to_num(
        working.get("avg_txn_amount_30days", 0), 0.0
    )
    working["distance_from_last_txn_km"] = to_num(
        working.get("distance_from_last_txn_km", 0), 0.0
    )
    working["account_age_days"] = to_num(working.get("account_age_days", 0), 0).astype(
        int
    )
    working["is_new_location"] = to_int_flag(working.get("is_new_location", 0))
    working["is_new_device"] = to_int_flag(working.get("is_new_device", 0))
    working["is_vpn_or_proxy"] = to_int_flag(working.get("is_vpn_or_proxy", 0))
    working["ip_matches_location"] = to_int_flag(working.get("ip_matches_location", 1))
    working["is_international"] = to_int_flag(working.get("is_international", 0))
    working["is_first_time_receiver"] = to_int_flag(
        working.get("is_first_time_receiver", 0)
    )

    merchant_enc, merchant_map = encode_series(
        working.get("merchant_category", "retail")
    )
    mode_enc, mode_map = encode_series(working.get("transaction_mode", "UPI"))
    location_enc, location_map = encode_series(working.get("location", "unknown"))
    iptag_enc, iptag_map = encode_series(working.get("ip_risk_tag", "CLEAN"))

    features = pd.DataFrame(
        {
            "amount": working["amount"],
            "balance": working["balance"],
            "txn_count_last_1hr": working["txn_count_last_1hr"],
            "txn_count_last_24hr": working["txn_count_last_24hr"],
            "avg_txn_amount_30days": working["avg_txn_amount_30days"],
            "distance_from_last_txn_km": working["distance_from_last_txn_km"],
            "account_age_days": working["account_age_days"],
            "is_new_location": working["is_new_location"],
            "is_new_device": working["is_new_device"],
            "is_vpn_or_proxy": working["is_vpn_or_proxy"],
            "ip_matches_location": working["ip_matches_location"],
            "is_international": working["is_international"],
            "is_first_time_receiver": working["is_first_time_receiver"],
            "merchant_enc": merchant_enc,
            "mode_enc": mode_enc,
            "location_enc": location_enc,
            "iptag_enc": iptag_enc,
        }
    )

    encoders = {
        "merchant": merchant_map,
        "mode": mode_map,
        "location": location_map,
        "iptag": iptag_map,
        "rule_cols": RULE_COLS,
    }
    return features, encoders


def main() -> int:
    parser = argparse.ArgumentParser(description="Train fraud and rule models")
    parser.add_argument("--input", required=True, help="Path to training CSV")
    parser.add_argument(
        "--models-dir", required=True, help="Directory to store model artifacts"
    )
    args = parser.parse_args()

    input_path = Path(args.input).resolve()
    models_dir = Path(args.models_dir).resolve()
    models_dir.mkdir(parents=True, exist_ok=True)

    if not input_path.exists():
        raise FileNotFoundError(f"Training input not found: {input_path}")

    df = pd.read_csv(input_path)
    if df.empty:
        raise ValueError("Training input is empty")

    if "is_fraud" not in df.columns:
        raise ValueError("Training input missing 'is_fraud' column")

    y = to_int_flag(df["is_fraud"])
    if y.nunique() < 2:
        raise ValueError("Need both fraud and non-fraud samples to train")

    X, encoders = build_feature_frame(df)

    fraud_model = RandomForestClassifier(
        n_estimators=260,
        random_state=42,
        class_weight="balanced_subsample",
        min_samples_leaf=2,
        n_jobs=-1,
    )
    fraud_model.fit(X, y)

    reason_col = (
        df["fraud_reason"]
        if "fraud_reason" in df.columns
        else pd.Series(["None"] * len(df))
    )
    rule_matrix = pd.DataFrame([rule_flags(value) for value in reason_col])

    rule_model = RandomForestClassifier(
        n_estimators=200,
        random_state=42,
        min_samples_leaf=1,
        n_jobs=-1,
    )
    rule_model.fit(X, rule_matrix)

    atomic_pickle(fraud_model, models_dir / "fraud_model.pkl")
    atomic_pickle(rule_model, models_dir / "rule_model.pkl")
    atomic_json(encoders, models_dir / "encoders.json")

    fraud_count = int(y.sum())
    clean_count = int(len(y) - fraud_count)
    print(
        json.dumps(
            {
                "status": "ok",
                "records": int(len(df)),
                "fraud_records": fraud_count,
                "clean_records": clean_count,
                "models_dir": str(models_dir),
            }
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
