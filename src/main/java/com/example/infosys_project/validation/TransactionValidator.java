package com.example.infosys_project.validation;

import com.example.infosys_project.model.TransactionModel;
import java.util.Arrays;
import java.util.List;

public class TransactionValidator {

    private static final List<String> VALID_LOCATIONS = Arrays.asList(
        // Indian cities
        "Mumbai","Delhi","Bangalore","Chennai","Hyderabad",
        "Pune","Kolkata","Ahmedabad","Noida","Gurgaon",
        "Jaipur","Lucknow","Chandigarh","Surat","Indore",
        "Bhopal","Nagpur","Patna","Bhubaneswar","Kochi",
        // International
        "London","Dubai","Singapore","New York","Tokyo",
        "Sydney","Toronto","Frankfurt","Paris","Amsterdam",
        "Hong Kong","Zurich","Stockholm","Seoul","Kuala Lumpur"
    );

    private static final List<String> VALID_DEVICES = Arrays.asList(
        "Android","iPhone","Desktop","ATM","Web",
        "iPad","MacBook","Linux Desktop","Windows Phone","Smart TV"
    );

    private static final List<String> VALID_CURRENCIES = Arrays.asList(
        "INR","USD","GBP","EUR","AED",
        "SGD","JPY","CAD","AUD","CHF"
    );

    private static final List<String> VALID_TYPES = Arrays.asList("debit","credit");

    private static final List<String> VALID_MODES = Arrays.asList(
        "UPI","NEFT","RTGS","IMPS","CARD"
    );

    private static final List<String> VALID_MERCHANTS = Arrays.asList(
        "grocery","retail","electronics","travel",
        "food","utilities","healthcare","education",
        "entertainment","insurance",
        "crypto","gambling","darkweb"   // allowed through; fraud rules handle these
    );

    private static final List<String> VALID_IP_RISK_TAGS = Arrays.asList(
        "CLEAN","VPN","PROXY","TOR","DATACENTER"
    );

    private static boolean isValidMobile(String mobile) {
        if (mobile == null || !mobile.startsWith("+91")) return false;
        String digits = mobile.substring(3);
        if (digits.length() != 10) return false;
        for (char c : digits.toCharArray())
            if (!Character.isDigit(c)) return false;
        return true;
    }

    public static String validate(TransactionModel t) {

        // 1. Name
        if (t.accountHolderName == null || t.accountHolderName.trim().isEmpty())
            return "Invalid Name: cannot be empty";
        for (char c : t.accountHolderName.trim().toCharArray())
            if (!Character.isLetter(c) && c != ' ')
                return "Invalid Name: only letters and spaces allowed";

        // 2. Mobile
        if (!isValidMobile(t.mobileNumber))
            return "Invalid Mobile: must be +91 followed by 10 digits";

        // 3. Sender Account
        if (t.senderAccount == null
                || t.senderAccount.length() < 9 || t.senderAccount.length() > 18)
            return "Invalid Sender Account: must be 9-18 digits";
        for (char c : t.senderAccount.toCharArray())
            if (!Character.isDigit(c))
                return "Invalid Sender Account: digits only";

        // 4. Receiver Account
        if (t.receiverAccount == null
                || t.receiverAccount.length() < 9 || t.receiverAccount.length() > 18)
            return "Invalid Receiver Account: must be 9-18 digits";
        for (char c : t.receiverAccount.toCharArray())
            if (!Character.isDigit(c))
                return "Invalid Receiver Account: digits only";

        // 5. Sender != Receiver
        if (t.senderAccount.equals(t.receiverAccount))
            return "Invalid: Sender and Receiver cannot be the same account";

        // 6. Transaction Type
        if (t.type == null || !VALID_TYPES.contains(t.type))
            return "Invalid Type: must be 'debit' or 'credit'";

        // 7. Amount
        if (t.amount <= 0)
            return "Invalid Amount: must be greater than zero";
        if (t.amount > 10_000_000)
            return "Invalid Amount: exceeds maximum limit of Rs 1 crore";

        // 8. Balance
        if (t.balance < 0)
            return "Invalid Balance: cannot be negative";

        // 9. Currency
        if (t.currency == null || !VALID_CURRENCIES.contains(t.currency))
            return "Invalid Currency: '" + t.currency + "' not supported";

        // 10. Location
        if (t.location == null || !VALID_LOCATIONS.contains(t.location))
            return "Invalid Location: '" + t.location + "' not recognised";

        // 11. Device
        if (t.device == null || !VALID_DEVICES.contains(t.device))
            return "Invalid Device: '" + t.device + "' not recognised";

        // 12. Transaction Mode
        if (t.transactionMode == null || !VALID_MODES.contains(t.transactionMode))
            return "Invalid Transaction Mode: must be UPI/NEFT/RTGS/IMPS/CARD";

        // 13. Merchant Category
        if (t.merchantCategory == null || !VALID_MERCHANTS.contains(t.merchantCategory))
            return "Invalid Merchant Category: '" + t.merchantCategory + "' not recognised";

        // 14. Account Age
        if (t.accountAgeDays < 0)
            return "Invalid Account Age: cannot be negative";

        // 15. Distance
        if (t.distanceFromLastTxnKm < 0)
            return "Invalid Distance: cannot be negative";

        // 16. IP Risk Tag
        if (t.ipRiskTag == null || !VALID_IP_RISK_TAGS.contains(t.ipRiskTag.toUpperCase()))
            return "Invalid IP Risk Tag: must be CLEAN/VPN/PROXY/TOR/DATACENTER";

        return "Valid Transaction Details";
    }
}
