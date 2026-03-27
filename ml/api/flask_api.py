"""Serve fraud models over a small Flask API.

Run with:
    ./run_ml.sh

Spring Boot calls POST /predict for each transaction. If ML is down,
the Java pipeline still runs with fallback scoring.
"""

from flask import Flask, request, jsonify
import pickle
import json
import pandas as pd
from pathlib import Path
import os

app = Flask(__name__)

BASE_DIR = Path(__file__).resolve().parent
ML_DIR = BASE_DIR.parent
MODELS_DIR = ML_DIR / "models"
MODEL_PATH = MODELS_DIR / "fraud_model.pkl"
RULE_MODEL_PATH = MODELS_DIR / "rule_model.pkl"
ENCODERS_PATH = MODELS_DIR / "encoders.json"


def _load_artifact(path, loader):
    if not path.exists():
        raise FileNotFoundError(f"Missing ML artifact: {path.name}")
    return loader(path)


# Load trained models and encoders from the ml folder regardless of current cwd.
model = _load_artifact(MODEL_PATH, lambda p: pickle.load(p.open("rb")))
rule_model = _load_artifact(RULE_MODEL_PATH, lambda p: pickle.load(p.open("rb")))
encoders = _load_artifact(
    ENCODERS_PATH, lambda p: json.load(p.open("r", encoding="utf-8"))
)
RULE_COLS = encoders.get(
    "rule_cols",
    [
        "r01",
        "r02",
        "r03",
        "r04",
        "r05",
        "r06",
        "r07",
        "r08",
        "r09",
        "r10",
        "r11",
        "r12",
        "r13",
        "r14",
    ],
)
FEATURE_COLS = [
    "amount",
    "balance",
    "txn_count_last_1hr",
    "txn_count_last_24hr",
    "avg_txn_amount_30days",
    "distance_from_last_txn_km",
    "account_age_days",
    "is_new_location",
    "is_new_device",
    "is_vpn_or_proxy",
    "ip_matches_location",
    "is_international",
    "is_first_time_receiver",
    "merchant_enc",
    "mode_enc",
    "location_enc",
    "iptag_enc",
]


def encode(value, mapping, default=0):
    return int(mapping.get(str(value), default))


def to_float(value, default=0.0):
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def to_int(value, default=0):
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def build_fraud_reason(flags, d):
    """Reconstruct a Java-style fraudReason string from predicted rule flags."""
    amount = to_float(d.get("amount", 0))
    balance = to_float(d.get("balance", 0))
    parts = []

    flag = dict(zip(RULE_COLS, flags))

    if flag.get("r01"):
        if amount >= 100000:
            parts.append(f"R01:CriticalAmount(={int(amount)})")
        else:
            parts.append(f"R01:HighAmount(={int(amount)})")

    if flag.get("r02"):
        parts.append("R02:OddHours")

    if flag.get("r03"):
        pct = round(amount / balance * 100) if balance > 0 else 0
        parts.append(f"R03:BalanceDrain(={pct}%)")

    if flag.get("r04"):
        cnt = to_int(d.get("txn_count_last_1hr", 0))
        label = "RapidFire" if cnt >= 8 else "FrequentTxns"
        parts.append(f"R04:{label}({cnt}txns/hr)")

    if flag.get("r05"):
        mc = d.get("merchant_category", "").lower()
        if mc == "crypto":
            parts.append("R05:CryptoMerchant")
        elif mc == "gambling":
            parts.append("R05:GamblingMerchant")
        elif mc == "darkweb":
            parts.append("R05:DarkWebMerchant")
        else:
            parts.append(f"R05:HighRiskMerchant({mc})")

    if flag.get("r06"):
        dist = to_float(d.get("distance_from_last_txn_km", 0))
        label = "ImpossibleTravel" if dist > 1000 else "SuspiciousLocationJump"
        parts.append(f"R06:{label}({int(dist)}km)")

    if flag.get("r07"):
        parts.append(f"R07:NewDeviceDetected({d.get('device', '')})")

    if flag.get("r08"):
        if to_int(d.get("is_vpn_or_proxy", 0)):
            parts.append("R08a:VPN/ProxyFlag")
        if not to_int(d.get("ip_matches_location", 1)):
            parts.append("R08b:IPLocationMismatch")
        tag = d.get("ip_risk_tag", "CLEAN").upper()
        if tag == "TOR":
            parts.append("R08c:TorNetwork")
        elif tag == "DATACENTER":
            parts.append("R08c:DatacenterIP(botSuspect)")
        elif tag == "PROXY":
            parts.append("R08c:AnonymousProxy")
        elif tag == "VPN":
            parts.append("R08c:CommercialVPN")

    if flag.get("r09"):
        parts.append(f"R09:InternationalTxn(currency={d.get('currency', 'INR')})")

    if flag.get("r10"):
        parts.append(f"R10:NewReceiverHighAmount({int(amount)})")

    if flag.get("r11"):
        avg = to_float(d.get("avg_txn_amount_30days", 1), 1.0) or 1.0
        mult = round(amount / avg)
        parts.append(f"R11:AmountSpike({mult}xAvg)")

    if flag.get("r12"):
        parts.append(
            f"R12:NewAccountLargeTransfer(age={to_int(d.get('account_age_days', 0))}days)"
        )

    if flag.get("r13"):
        parts.append(
            f"R13:HighDailyVolume({to_int(d.get('txn_count_last_24hr', 0))}txns)"
        )

    if flag.get("r14"):
        parts.append(f"R14:RoundAmountStructuring({int(amount)})")

    return " | ".join(parts) if parts else "None"


@app.route("/health", methods=["GET"])
def health():
    return jsonify(
        {
            "status": "running",
            "model": "RandomForest",
            "rule_model": "MultiLabel-RandomForest",
            "artifacts": {
                "fraud_model": MODEL_PATH.exists(),
                "rule_model": RULE_MODEL_PATH.exists(),
                "encoders": ENCODERS_PATH.exists(),
            },
        }
    )


@app.route("/predict", methods=["POST"])
def predict():
    d = request.get_json(silent=True)
    if not isinstance(d, dict):
        return jsonify({"error": "Invalid JSON body"}), 400

    merchant_map = encoders.get("merchant", {})
    mode_map = encoders.get("mode", {})
    location_map = encoders.get("location", {})
    iptag_map = encoders.get("iptag", {})

    # Build feature vector in the same order as training.
    features = [
        to_float(d.get("amount", 0)),
        to_float(d.get("balance", 0)),
        to_int(d.get("txn_count_last_1hr", 0)),
        to_int(d.get("txn_count_last_24hr", 0)),
        to_float(d.get("avg_txn_amount_30days", 0)),
        to_float(d.get("distance_from_last_txn_km", 0)),
        to_int(d.get("account_age_days", 0)),
        to_int(d.get("is_new_location", 0)),
        to_int(d.get("is_new_device", 0)),
        to_int(d.get("is_vpn_or_proxy", 0)),
        to_int(d.get("ip_matches_location", 1)),
        to_int(d.get("is_international", 0)),
        to_int(d.get("is_first_time_receiver", 0)),
        encode(
            d.get("merchant_category", "retail"),
            merchant_map,
            merchant_map.get("retail", 0),
        ),
        encode(d.get("transaction_mode", "UPI"), mode_map, mode_map.get("UPI", 0)),
        encode(d.get("location", "Delhi"), location_map, location_map.get("Delhi", 0)),
        encode(d.get("ip_risk_tag", "CLEAN"), iptag_map, iptag_map.get("CLEAN", 0)),
    ]

    try:
        X = pd.DataFrame([features], columns=FEATURE_COLS)
        prob = round(float(model.predict_proba(X)[0][1]), 4)
    except Exception as exc:
        return jsonify({"error": f"ML inference failed: {exc}"}), 500

    if prob >= 0.80:
        risk = "CRITICAL"
    elif prob >= 0.60:
        risk = "HIGH"
    elif prob >= 0.40:
        risk = "MEDIUM"
    else:
        risk = "NORMAL"

    # Predict the 14 rule flags as a companion explanation model.
    rule_flags = rule_model.predict(X)[0].tolist()
    fired_rules = {col: int(rule_flags[i]) for i, col in enumerate(RULE_COLS)}
    fraud_reason = build_fraud_reason(rule_flags, d)

    return jsonify(
        {
            "fraud_probability": prob,
            "ml_risk_level": risk,
            "is_fraud_ml": prob >= 0.60,
            "fraud_reason": fraud_reason,
            "fired_rules": fired_rules,
        }
    )


if __name__ == "__main__":
    port = int(os.getenv("PORT", "5000"))
    print(f"Flask ML API starting on port {port}...")
    print(f"Health check: GET http://localhost:{port}/health")
    print(f"Predict:      POST http://localhost:{port}/predict")
    app.run(host="0.0.0.0", port=port, debug=False)
