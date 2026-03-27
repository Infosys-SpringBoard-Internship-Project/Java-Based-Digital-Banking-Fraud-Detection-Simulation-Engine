package com.example.infosys_project.detection;

import com.example.infosys_project.model.TransactionModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FraudDetector {

    /*
     * Risk score range is 0.0 to 10.0 (capped).
     * NORMAL: 0.0-0.8, MEDIUM: 0.9-4.1, HIGH: 4.2-8.1, CRITICAL: 8.2-10.0.
     */

    public static void checkFraud(TransactionModel tx) {

        double score = 0.0;
        List<String> rules = new ArrayList<>();

        // Rule 1: high-value transaction.
        if (tx.amount >= 100000) {
            score += 4.0;
            rules.add("R01:CriticalAmount(=" + (int) tx.amount + ")");
        } else if (tx.amount >= 50000) {
            score += 2.5;
            rules.add("R01:HighAmount(=" + (int) tx.amount + ")");
        }

        // Rule 2: odd hours (1 AM to 4 AM).
        int hour = tx.timestamp != null ? tx.timestamp.getHour() : LocalDateTime.now().getHour();
        if (hour >= 1 && hour < 5) {
            score += 2.0;
            rules.add("R02:OddHours(hour=" + hour + ")");
        }

        // Rule 3: full or near-full balance drain.
        if (tx.type != null && tx.type.equals("debit")
                && tx.balance > 0
                && tx.amount >= tx.balance * 0.90) {
            score += 3.5;
            rules.add("R03:BalanceDrain(=" + Math.round(tx.amount / tx.balance * 100) + "%)");
        }

        // Rule 4: rapid-fire transactions.
        if (tx.txnCountLastHour >= 8) {
            score += 3.0;
            rules.add("R04:RapidFire(" + tx.txnCountLastHour + "txns/hr)");
        } else if (tx.txnCountLastHour >= 4) {
            score += 1.5;
            rules.add("R04:FrequentTxns(" + tx.txnCountLastHour + "txns/hr)");
        }

        // Rule 5: high-risk merchant categories.
        if (tx.merchantCategory != null) {
            switch (tx.merchantCategory.toLowerCase()) {
                case "crypto":
                    score += 3.0;
                    rules.add("R05:CryptoMerchant");
                    break;
                case "gambling":
                    score += 2.5;
                    rules.add("R05:GamblingMerchant");
                    break;
                case "darkweb":
                    score += 5.0;
                    rules.add("R05:DarkWebMerchant");
                    break;
                default:
                    break;
            }
        }

        // Rule 6: impossible travel or large location jump.
        if (tx.isNewLocation) {
            if (tx.distanceFromLastTxnKm > 1000) {
                score += 3.5;
                rules.add("R06:ImpossibleTravel(" + (int) tx.distanceFromLastTxnKm + "km)");
            } else if (tx.distanceFromLastTxnKm > 500) {
                score += 2.0;
                rules.add("R06:SuspiciousLocationJump(" + (int) tx.distanceFromLastTxnKm + "km)");
            }
        }

        // Rule 7: new device detected.
        if (tx.isNewDevice) {
            score += 1.5;
            rules.add("R07:NewDeviceDetected(" + tx.device + ")");
        }

        // Rule 8: IP intelligence sub-rules.
        // R08a: VPN/proxy flag set on the transaction.
        if (tx.isVpnOrProxy) {
            score += 1.5;
            rules.add("R08a:VPN/ProxyFlag");
        }
        // R08b: IP country differs from transaction location.
        if (!tx.ipMatchesLocation) {
            score += 2.0;
            rules.add("R08b:IPLocationMismatch(ip=" + tx.ipCountry
                      + ",txn=" + tx.location + ")");
        }
        // R08c: risk tag scoring.
        if (tx.ipRiskTag != null) {
            switch (tx.ipRiskTag.toUpperCase()) {
                case "TOR":
                    score += 3.5;
                    rules.add("R08c:TorNetwork");
                    break;
                case "DATACENTER":
                    score += 2.5;
                    rules.add("R08c:DatacenterIP(botSuspect)");
                    break;
                case "PROXY":
                    score += 2.0;
                    rules.add("R08c:AnonymousProxy");
                    break;
                case "VPN":
                    score += 1.0;
                    rules.add("R08c:CommercialVPN");
                    break;
                default:
                    break; // CLEAN adds no score.
            }
        }

        // Rule 9: unexpected international transaction.
        if (tx.isInternational && tx.currency != null && !tx.currency.equals("INR")) {
            score += 2.0;
            rules.add("R09:InternationalTxn(currency=" + tx.currency + ")");
        }

        // Rule 10: first-time receiver with high amount.
        if (tx.isFirstTimeReceiver && tx.amount >= 20000) {
            score += 2.0;
            rules.add("R10:NewReceiverHighAmount(" + (int) tx.amount + ")");
        }

        // Rule 11: amount spike vs 30-day average.
        if (tx.avgTxnAmount30Days > 0 && tx.amount >= tx.avgTxnAmount30Days * 5) {
            score += 2.5;
            rules.add("R11:AmountSpike("
                    + Math.round(tx.amount / tx.avgTxnAmount30Days) + "xAvg)");
        }

        // Rule 12: new account with large transfer.
        if (tx.accountAgeDays < 30 && tx.amount >= 10000) {
            score += 2.5;
            rules.add("R12:NewAccountLargeTransfer(age=" + tx.accountAgeDays + "days)");
        }

        // Rule 13: high daily transaction volume.
        if (tx.txnCountLast24Hours >= 20) {
            score += 2.0;
            rules.add("R13:HighDailyVolume(" + tx.txnCountLast24Hours + "txns)");
        }

        // Rule 14: round-number structuring.
        if (tx.amount >= 10000 && tx.amount % 1000 == 0) {
            score += 1.0;
            rules.add("R14:RoundAmountStructuring(" + (int) tx.amount + ")");
        }

        // Cap score at 10.0 and round to one decimal place.
        score        = Math.min(score, 10.0);
        tx.riskScore = Math.round(score * 10.0) / 10.0;

        // Assign final risk level and fraud flag.
        if (tx.riskScore >= 8.2) {
            tx.riskLevel = "CRITICAL";
            tx.isFraud   = true;
        } else if (tx.riskScore >= 4.2) {
            tx.riskLevel = "HIGH";
            tx.isFraud   = true;
        } else if (tx.riskScore >= 0.9) {
            tx.riskLevel = "MEDIUM";
            tx.isFraud   = false;
        } else {
            tx.riskLevel = "NORMAL";
            tx.isFraud   = false;
        }

        tx.fraudReason = rules.isEmpty() ? "None" : String.join(" | ", rules);
    }
}
