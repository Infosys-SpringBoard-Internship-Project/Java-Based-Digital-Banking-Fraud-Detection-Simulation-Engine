package com.example.infosys_project.generator;

import com.example.infosys_project.model.TransactionModel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

public class TransactionGenerator {

    static final Random random = new Random();

    // Synthetic data pools used by the generator.
    static final String[] NAMES = {
        "Aarav Shah",       "Diya Mehta",        "Arjun Verma",       "Meera Nair",
        "Rohan Gupta",      "Sneha Pillai",       "Karan Joshi",       "Priya Singh",
        "Vikram Rao",       "Anjali Desai",       "Rahul Khanna",      "Pooja Iyer",
        "Siddharth Malhotra","Neha Sharma",       "Aditya Kumar",      "Riya Patel",
        "Manish Tiwari",    "Kavya Reddy",        "Nikhil Bansal",     "Shreya Bose",
        "Tanmay Kulkarni",  "Ishita Chaudhary",   "Yash Agarwal",      "Divya Menon",
        "Abhishek Nair",    "Simran Kapoor",      "Varun Saxena",      "Ananya Mishra"
    };

    static final String[] INDIAN_LOCATIONS = {
        "Mumbai", "Delhi", "Bangalore", "Chennai", "Hyderabad",
        "Pune",   "Kolkata","Ahmedabad","Noida",   "Gurgaon",
        "Jaipur", "Lucknow","Chandigarh","Surat",  "Indore",
        "Bhopal", "Nagpur", "Patna",    "Bhubaneswar","Kochi"
    };

    static final String[] INTERNATIONAL_LOCATIONS = {
        "London",    "Dubai",     "Singapore", "New York",  "Tokyo",
        "Sydney",    "Toronto",   "Frankfurt", "Paris",     "Amsterdam",
        "Hong Kong", "Zurich",    "Stockholm", "Seoul",     "Kuala Lumpur"
    };

    static final String[] DEVICES = {
        "Android", "iPhone", "Web",     "ATM",     "Desktop",
        "iPad",    "MacBook","Linux Desktop","Windows Phone","Smart TV"
    };

    static final String[] BANKS = {
        "HDFC",         "SBI",          "ICICI",   "Axis",        "Kotak",
        "PNB",          "Bank of Baroda","Canara Bank","IDBI",    "IndusInd",
        "Yes Bank",     "Federal Bank", "Union Bank","RBL Bank",  "IDFC First"
    };

    static final String[] CURRENCIES = {
        "INR", "USD", "GBP", "EUR", "AED",
        "SGD", "JPY", "CAD", "AUD", "CHF"
    };

    static final String[] NORMAL_MERCHANTS = {
        "grocery", "retail",        "electronics", "travel",
        "food",    "utilities",     "healthcare",  "education",
        "entertainment", "insurance"
    };

    static final String[] HIGH_RISK_MERCHANTS = {
        "crypto", "gambling", "darkweb"
    };

    static final String[] TRANSACTION_MODES = {
        "UPI", "NEFT", "RTGS", "IMPS", "CARD"
    };

    // Simulated datacenter and VPN IP prefixes.
    static final String[] DATACENTER_PREFIXES = {
        "185.220", "104.21",  "172.67",  "45.33",   "198.54",
        "162.158", "141.101", "108.162", "190.93",  "103.21"
    };

    static final String[] CLEAN_IP_PREFIXES = {
        "49.36",  "103.107", "117.96",  "59.144",  "122.161",
        "14.139", "203.122", "27.56",   "182.72",  "106.210"
    };

    // Helper methods.

    static String generatePhone() {
        return "+91" + (6000000000L + (long) random.nextInt(399999999));
    }

    static String generateAccount() {
        int len = 9 + random.nextInt(10);
        StringBuilder sb = new StringBuilder();
        sb.append(1 + random.nextInt(9));
        for (int i = 1; i < len; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }

    static String generateMerchantId(String category) {
        String prefix = category.substring(0, Math.min(3, category.length())).toUpperCase();
        return "MRC-" + prefix + "-" + (1000 + random.nextInt(9000));
    }

    /**
     * Returns [ipAddress, ipRiskTag]
     * tag: CLEAN | VPN | PROXY | TOR | DATACENTER
     */
    static String[] generateIpWithTag(String tag) {
        String ip;
        switch (tag) {
            case "TOR":
                ip = "185.220." + random.nextInt(256) + "." + random.nextInt(256);
                break;
            case "DATACENTER":
                String dc = DATACENTER_PREFIXES[random.nextInt(DATACENTER_PREFIXES.length)];
                ip = dc + "." + random.nextInt(256) + "." + random.nextInt(256);
                break;
            case "VPN":
            case "PROXY":
                ip = "104." + random.nextInt(256) + "."
                           + random.nextInt(256) + "." + random.nextInt(256);
                break;
            default: // CLEAN
                String clean = CLEAN_IP_PREFIXES[random.nextInt(CLEAN_IP_PREFIXES.length)];
                ip = clean + "." + random.nextInt(256) + "." + random.nextInt(256);
                break;
        }
        return new String[]{ip, tag};
    }

    /**
     * For CLEAN IPs the country matches the txn location.
     * For suspicious IPs the country is a random foreign country (mismatch).
     */
    static String resolveIpCountry(String ipRiskTag, String txnLocation) {
        if ("CLEAN".equals(ipRiskTag)) {
            if (isIndianCity(txnLocation))               return "India";
            if ("London".equals(txnLocation))            return "UK";
            if ("Dubai".equals(txnLocation))             return "UAE";
            if ("Singapore".equals(txnLocation))         return "Singapore";
            if ("New York".equals(txnLocation)
                    || "Toronto".equals(txnLocation))    return "USA";
            if ("Tokyo".equals(txnLocation))             return "Japan";
            if ("Sydney".equals(txnLocation))            return "Australia";
            if ("Frankfurt".equals(txnLocation)
                    || "Paris".equals(txnLocation)
                    || "Amsterdam".equals(txnLocation)
                    || "Zurich".equals(txnLocation)
                    || "Stockholm".equals(txnLocation))  return "Europe";
            if ("Hong Kong".equals(txnLocation)
                    || "Seoul".equals(txnLocation)
                    || "Kuala Lumpur".equals(txnLocation)) return "Asia";
            return "India";
        }
        // Suspicious IPs intentionally map to mismatched foreign countries.
        String[] foreign = {
            "Netherlands", "Romania", "Russia", "Germany",
            "Panama",      "Seychelles", "Bulgaria", "Moldova"
        };
        return foreign[random.nextInt(foreign.length)];
    }

    static boolean isIndianCity(String location) {
        for (String city : INDIAN_LOCATIONS) {
            if (city.equals(location)) return true;
        }
        return false;
    }

    // Main entry point.

    /**
     * Distribution target:
     * NORMAL 75% | MEDIUM 12% | HIGH 8% | CRITICAL 5%
     */
    public static TransactionModel generateTransaction() {
        int roll = random.nextInt(100);
        if (roll < 75) {
            return generateNormalTransaction();
        }
        if (roll < 87) {
            return generateMediumRiskTransaction();
        }
        if (roll < 95) {
            return generateHighRiskTransaction();
        }
        return generateCriticalRiskTransaction();
    }

    public static TransactionModel generateTransactionForScenario(List<String> scenarios,
                                                                 boolean forceFraud,
                                                                 boolean forceMedium,
                                                                 Double amountMin,
                                                                 Double amountMax) {
        TransactionModel tx;
        if (forceFraud) {
            tx = (scenarios == null || scenarios.isEmpty())
                    ? generateWeightedFraudTransaction()
                    : generateScenarioTransaction(scenarios);
        } else if (forceMedium) {
            tx = generateMediumRiskTransaction();
        } else {
            // Keep non-fraud traffic mixed: ~86.2% NORMAL, ~13.8% MEDIUM
            tx = random.nextInt(1000) < 862
                    ? generateNormalTransaction()
                    : generateMediumRiskTransaction();
        }

        double min = amountMin == null ? 0.0 : Math.max(0.0, amountMin);
        double max = amountMax == null ? Math.max(min + 1000.0, 100000.0) : Math.max(amountMax, min + 1.0);
        tx.amount = Math.max(min, Math.min(max, tx.amount));
        tx.balance = Math.max(tx.amount * 1.02, tx.balance);
        return tx;
    }

    private static TransactionModel generateScenarioTransaction(List<String> scenarios) {
        if (scenarios == null || scenarios.isEmpty()) {
            return generateFraudTransaction();
        }
        String scenario = scenarios.get(random.nextInt(scenarios.size()));
        return switch (scenario.toUpperCase()) {
            case "HIGH_AMOUNT" -> fraudHighAmount();
            case "RAPID_FIRE", "VPN" -> fraudRapidVpn();
            case "TOR", "INTERNATIONAL" -> fraudTorInternational();
            case "CRYPTO", "GAMBLING", "DATACENTER" -> fraudRiskMerchant();
            case "AMOUNT_SPIKE", "NEW_RECEIVER" -> fraudAmountSpike();
            case "PROXY", "NEW_LOCATION", "HIGH_VOLUME" -> fraudProxyVolume();
            case "MAX_CHAOS" -> fraudMaxChaos();
            default -> generateFraudTransaction();
        };
    }

    // Normal transaction generation.
    static TransactionModel generateNormalTransaction() {
        String loc    = INDIAN_LOCATIONS[random.nextInt(INDIAN_LOCATIONS.length)];
        String prev   = INDIAN_LOCATIONS[random.nextInt(INDIAN_LOCATIONS.length)];
        double avg    = 1000 + random.nextInt(8000);
        double amount = Math.round((avg * (0.3 + random.nextDouble())) * 100.0) / 100.0;
        double balance= Math.round((amount * 4 + random.nextInt(50000)) * 100.0) / 100.0;
        String[] ip   = generateIpWithTag("CLEAN");

        TransactionModel tx = new TransactionModel(
            NAMES[random.nextInt(NAMES.length)],
            generatePhone(),
            generateAccount(), generateAccount(),
            BANKS[random.nextInt(BANKS.length)],
            365 + random.nextInt(2000),
            "debit", amount, balance, "INR",
            NORMAL_MERCHANTS[random.nextInt(NORMAL_MERCHANTS.length)],
            generateMerchantId("retail"),
            TRANSACTION_MODES[random.nextInt(TRANSACTION_MODES.length)],
            loc, prev,
            !loc.equals(prev) && random.nextInt(10) < 1,
            10 + random.nextInt(150),
            DEVICES[random.nextInt(DEVICES.length)],
            false,
            ip[0], false,
            resolveIpCountry("CLEAN", loc), true, "CLEAN",
            random.nextInt(3),
            random.nextInt(6),
            Math.round(avg * 100.0) / 100.0,
            false, false
        );
        assignTimestamp(tx, true);
        return tx;
    }

    static TransactionModel generateMediumRiskTransaction() {
        String loc = INDIAN_LOCATIONS[random.nextInt(INDIAN_LOCATIONS.length)];
        String[] ip = generateIpWithTag("CLEAN");
        TransactionModel tx = new TransactionModel(
                NAMES[random.nextInt(NAMES.length)],
                generatePhone(),
                generateAccount(),
                generateAccount(),
                BANKS[random.nextInt(BANKS.length)],
                180 + random.nextInt(700),
                "debit",
                12000 + random.nextInt(18000),
                120000 + random.nextInt(160000),
                "INR",
                "retail",
                generateMerchantId("retail"),
                TRANSACTION_MODES[random.nextInt(TRANSACTION_MODES.length)],
                loc,
                loc,
                false,
                0,
                DEVICES[random.nextInt(DEVICES.length)],
                true,
                ip[0],
                false,
                "India",
                true,
                "CLEAN",
                4,
                7,
                5000.0,
                false,
                false
        );
        assignTimestamp(tx, false);
        return tx;
    }

    static TransactionModel generateHighRiskTransaction() {
        String loc = INDIAN_LOCATIONS[random.nextInt(INDIAN_LOCATIONS.length)];
        String[] ip = generateIpWithTag("VPN");
        TransactionModel tx = new TransactionModel(
                NAMES[random.nextInt(NAMES.length)],
                generatePhone(),
                generateAccount(),
                generateAccount(),
                BANKS[random.nextInt(BANKS.length)],
                120 + random.nextInt(500),
                "debit",
                50000 + random.nextInt(25000),
                220000 + random.nextInt(260000),
                "INR",
                "electronics",
                generateMerchantId("electronics"),
                "UPI",
                loc,
                loc,
                false,
                0,
                DEVICES[random.nextInt(DEVICES.length)],
                true,
                ip[0],
                true,
                resolveIpCountry("VPN", loc),
                false,
                "VPN",
                8,
                15,
                7000.0,
                false,
                true
        );
        assignTimestamp(tx, false);
        return tx;
    }

    static TransactionModel generateCriticalRiskTransaction() {
        TransactionModel tx = fraudMaxChaos();
        assignTimestamp(tx, false);
        return tx;
    }

    static TransactionModel generateWeightedFraudTransaction() {
        // fraud bucket split: HIGH 8, CRITICAL 5
        return random.nextInt(13) < 8
                ? generateHighRiskTransaction()
                : generateCriticalRiskTransaction();
    }

    // Fraud transaction generation with seven scenarios.
    static TransactionModel generateFraudTransaction() {
        switch (random.nextInt(7)) {

            case 0: return fraudHighAmount();

            case 1: return fraudRapidVpn();

            case 2: return fraudTorInternational();

            case 3: return fraudRiskMerchant();

            case 4: return fraudAmountSpike();

            case 5: return fraudProxyVolume();

            default: return fraudMaxChaos();
        }
    }

    static void assignTimestamp(TransactionModel tx, boolean preferSafeHours) {
        int hour;
        if (preferSafeHours) {
            int[] safeHours = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
            hour = safeHours[random.nextInt(safeHours.length)];
        } else {
            hour = random.nextInt(24);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime ts = now.withHour(hour)
                .withMinute(random.nextInt(60))
                .withSecond(random.nextInt(60))
                .withNano(0);
        if (ts.isAfter(now)) {
            ts = ts.minusDays(1);
        }
        tx.timestamp = ts;
    }

    private static TransactionModel fraudHighAmount() {
        double big = ((50 + random.nextInt(50)) * 1000.0);
        String loc = INDIAN_LOCATIONS[random.nextInt(INDIAN_LOCATIONS.length)];
        String[] ip = generateIpWithTag("CLEAN");
        return new TransactionModel(
                NAMES[random.nextInt(NAMES.length)], generatePhone(), generateAccount(), generateAccount(),
                BANKS[random.nextInt(BANKS.length)], 200 + random.nextInt(500),
                "debit", big, big * 1.05, "INR", "electronics", generateMerchantId("electronics"),
                TRANSACTION_MODES[random.nextInt(TRANSACTION_MODES.length)], loc, loc, false, 0,
                DEVICES[random.nextInt(DEVICES.length)], false, ip[0], false, "India", true, "CLEAN",
                1, 3, big / 15.0, false, true
        );
    }

    private static TransactionModel fraudRapidVpn() {
        String loc = INDIAN_LOCATIONS[random.nextInt(INDIAN_LOCATIONS.length)];
        String[] ip = generateIpWithTag("VPN");
        return new TransactionModel(
                NAMES[random.nextInt(NAMES.length)], generatePhone(), generateAccount(), generateAccount(),
                BANKS[random.nextInt(BANKS.length)], 90 + random.nextInt(200),
                "debit", 5000 + random.nextInt(15000), 100000 + random.nextInt(200000), "INR",
                "retail", generateMerchantId("retail"), "UPI", loc, loc, false, 0,
                DEVICES[random.nextInt(DEVICES.length)], true, ip[0], true,
                resolveIpCountry("VPN", loc), false, "VPN", 8 + random.nextInt(6), 20 + random.nextInt(10),
                3000.0, false, false
        );
    }

    private static TransactionModel fraudTorInternational() {
        String loc = INTERNATIONAL_LOCATIONS[random.nextInt(INTERNATIONAL_LOCATIONS.length)];
        String prev = INDIAN_LOCATIONS[random.nextInt(INDIAN_LOCATIONS.length)];
        String[] ip = generateIpWithTag("TOR");
        String curr = CURRENCIES[1 + random.nextInt(CURRENCIES.length - 1)];
        return new TransactionModel(
                NAMES[random.nextInt(NAMES.length)], generatePhone(), generateAccount(), generateAccount(),
                BANKS[random.nextInt(BANKS.length)], 500 + random.nextInt(1000),
                "debit", 15000 + random.nextInt(40000), 300000 + random.nextInt(200000), curr,
                "travel", generateMerchantId("travel"), "CARD", loc, prev, true, 1200 + random.nextInt(6000),
                DEVICES[random.nextInt(DEVICES.length)], true, ip[0], true,
                resolveIpCountry("TOR", loc), false, "TOR", 2, 5, 8000.0, true, false
        );
    }

    private static TransactionModel fraudRiskMerchant() {
        String loc = INDIAN_LOCATIONS[random.nextInt(INDIAN_LOCATIONS.length)];
        String[] ip = generateIpWithTag("DATACENTER");
        String merch = HIGH_RISK_MERCHANTS[random.nextInt(2)];
        return new TransactionModel(
                NAMES[random.nextInt(NAMES.length)], generatePhone(), generateAccount(), generateAccount(),
                BANKS[random.nextInt(BANKS.length)], 3 + random.nextInt(25),
                "debit", 20000 + random.nextInt(50000), 60000 + random.nextInt(100000), "INR",
                merch, generateMerchantId(merch), "IMPS", loc, loc, false, 0,
                DEVICES[random.nextInt(DEVICES.length)], false, ip[0], true,
                resolveIpCountry("DATACENTER", loc), false, "DATACENTER", 3, 8, 2000.0, false, true
        );
    }

    private static TransactionModel fraudAmountSpike() {
        double avg = 2000 + random.nextInt(3000);
        String loc = INDIAN_LOCATIONS[random.nextInt(INDIAN_LOCATIONS.length)];
        String[] ip = generateIpWithTag("CLEAN");
        return new TransactionModel(
                NAMES[random.nextInt(NAMES.length)], generatePhone(), generateAccount(), generateAccount(),
                BANKS[random.nextInt(BANKS.length)], 700 + random.nextInt(1000),
                "debit", avg * 10, avg * 14, "INR", "electronics", generateMerchantId("electronics"), "NEFT",
                loc, loc, false, 0, DEVICES[random.nextInt(DEVICES.length)], false,
                ip[0], false, "India", true, "CLEAN", 1, 2, avg, false, true
        );
    }

    private static TransactionModel fraudProxyVolume() {
        String loc = INDIAN_LOCATIONS[random.nextInt(INDIAN_LOCATIONS.length)];
        String prev = INDIAN_LOCATIONS[random.nextInt(INDIAN_LOCATIONS.length)];
        String[] ip = generateIpWithTag("PROXY");
        return new TransactionModel(
                NAMES[random.nextInt(NAMES.length)], generatePhone(), generateAccount(), generateAccount(),
                BANKS[random.nextInt(BANKS.length)], 180 + random.nextInt(300),
                "debit", 8000 + random.nextInt(20000), 80000 + random.nextInt(150000), "INR",
                "retail", generateMerchantId("retail"), "UPI", loc, prev,
                !loc.equals(prev), 600 + random.nextInt(800), DEVICES[random.nextInt(DEVICES.length)], true,
                ip[0], true, resolveIpCountry("PROXY", loc), false, "PROXY", 5, 22 + random.nextInt(8),
                4000.0, false, false
        );
    }

    private static TransactionModel fraudMaxChaos() {
        String loc = INTERNATIONAL_LOCATIONS[random.nextInt(INTERNATIONAL_LOCATIONS.length)];
        String prev = INDIAN_LOCATIONS[random.nextInt(INDIAN_LOCATIONS.length)];
        String[] ip = generateIpWithTag("TOR");
        double big = (60 + random.nextInt(40)) * 1000.0;
        return new TransactionModel(
                NAMES[random.nextInt(NAMES.length)], generatePhone(), generateAccount(), generateAccount(),
                BANKS[random.nextInt(BANKS.length)], 5 + random.nextInt(15),
                "debit", big, big * 1.02, "USD", "gambling", generateMerchantId("gambling"), "CARD",
                loc, prev, true, 1500 + random.nextInt(4000), "iPhone", true,
                ip[0], true, resolveIpCountry("TOR", loc), false, "TOR", 9 + random.nextInt(6),
                25 + random.nextInt(10), 5000.0, true, true
        );
    }
}
