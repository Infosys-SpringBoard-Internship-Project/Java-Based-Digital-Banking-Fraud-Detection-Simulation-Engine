package com.example.infosys_project.security;

import com.example.infosys_project.model.TransactionModel;
import com.example.infosys_project.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DataMaskingUtil}.
 * Tests data masking functionality for ANALYST role users.
 */
@DisplayName("DataMaskingUtil Unit Tests")
class DataMaskingUtilTest {

    @Nested
    @DisplayName("shouldMaskData tests")
    class ShouldMaskDataTests {

        @Test
        @DisplayName("Should return true for ANALYST role")
        void shouldReturnTrueForAnalyst() {
            assertThat(DataMaskingUtil.shouldMaskData(UserRole.ANALYST)).isTrue();
        }

        @Test
        @DisplayName("Should return false for ADMIN role")
        void shouldReturnFalseForAdmin() {
            assertThat(DataMaskingUtil.shouldMaskData(UserRole.ADMIN)).isFalse();
        }

        @Test
        @DisplayName("Should return false for SUPERADMIN role")
        void shouldReturnFalseForSuperadmin() {
            assertThat(DataMaskingUtil.shouldMaskData(UserRole.SUPERADMIN)).isFalse();
        }
    }

    @Nested
    @DisplayName("maskName tests")
    class MaskNameTests {

        @Test
        @DisplayName("Should mask 'John Doe' as 'J*** D**'")
        void shouldMaskFullName() {
            String masked = DataMaskingUtil.maskName("John Doe");
            assertThat(masked).isEqualTo("J*** D**");
        }

        @Test
        @DisplayName("Should mask single name 'John' as 'J***'")
        void shouldMaskSingleName() {
            String masked = DataMaskingUtil.maskName("John");
            assertThat(masked).isEqualTo("J***");
        }

        @Test
        @DisplayName("Should mask three-part name 'John William Doe'")
        void shouldMaskThreePartName() {
            String masked = DataMaskingUtil.maskName("John William Doe");
            assertThat(masked).isEqualTo("J*** W****** D**");
        }

        @Test
        @DisplayName("Should handle single character name 'A'")
        void shouldHandleSingleCharacterName() {
            String masked = DataMaskingUtil.maskName("A");
            assertThat(masked).isEqualTo("A");
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            assertThat(DataMaskingUtil.maskName(null)).isNull();
        }

        @Test
        @DisplayName("Should return empty string for empty input")
        void shouldReturnEmptyForEmptyInput() {
            assertThat(DataMaskingUtil.maskName("")).isEmpty();
        }

        @Test
        @DisplayName("Should handle names with multiple spaces")
        void shouldHandleMultipleSpaces() {
            String masked = DataMaskingUtil.maskName("John   Doe");
            assertThat(masked).isEqualTo("J*** D**");
        }
    }

    @Nested
    @DisplayName("maskMobileNumber tests")
    class MaskMobileNumberTests {

        @Test
        @DisplayName("Should mask '9876543210' as '98****3210'")
        void shouldMaskTenDigitNumber() {
            String masked = DataMaskingUtil.maskMobileNumber("9876543210");
            assertThat(masked).isEqualTo("98****3210");
        }

        @Test
        @DisplayName("Should mask 12-digit number correctly")
        void shouldMaskTwelveDigitNumber() {
            String masked = DataMaskingUtil.maskMobileNumber("919876543210");
            assertThat(masked).isEqualTo("91******3210");
        }

        @Test
        @DisplayName("Should return short number unchanged if less than 6 digits")
        void shouldReturnShortNumberUnchanged() {
            assertThat(DataMaskingUtil.maskMobileNumber("12345")).isEqualTo("12345");
        }

        @Test
        @DisplayName("Should handle exactly 6 digit number")
        void shouldHandleSixDigitNumber() {
            String masked = DataMaskingUtil.maskMobileNumber("123456");
            assertThat(masked).isEqualTo("123456");
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            assertThat(DataMaskingUtil.maskMobileNumber(null)).isNull();
        }
    }

    @Nested
    @DisplayName("maskAccountNumber tests")
    class MaskAccountNumberTests {

        @Test
        @DisplayName("Should mask '123456789012' as '1234****9012'")
        void shouldMaskTwelveDigitAccount() {
            String masked = DataMaskingUtil.maskAccountNumber("123456789012");
            assertThat(masked).isEqualTo("1234****9012");
        }

        @Test
        @DisplayName("Should mask 16-digit account correctly")
        void shouldMaskSixteenDigitAccount() {
            String masked = DataMaskingUtil.maskAccountNumber("1234567890123456");
            assertThat(masked).isEqualTo("1234********3456");
        }

        @Test
        @DisplayName("Should handle exactly 8 digit account")
        void shouldHandleEightDigitAccount() {
            String masked = DataMaskingUtil.maskAccountNumber("12345678");
            assertThat(masked).isEqualTo("12345678");
        }

        @Test
        @DisplayName("Should return short account unchanged if less than 8 digits")
        void shouldReturnShortAccountUnchanged() {
            assertThat(DataMaskingUtil.maskAccountNumber("1234567")).isEqualTo("1234567");
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            assertThat(DataMaskingUtil.maskAccountNumber(null)).isNull();
        }
    }

    @Nested
    @DisplayName("maskIpAddress tests")
    class MaskIpAddressTests {

        @Test
        @DisplayName("Should mask IPv4 '192.168.1.100' as '192.***.***.***'")
        void shouldMaskIpv4Address() {
            String masked = DataMaskingUtil.maskIpAddress("192.168.1.100");
            assertThat(masked).isEqualTo("192.***.***." + "***");
        }

        @Test
        @DisplayName("Should mask IPv4 with single-digit octets")
        void shouldMaskIpv4WithSingleDigits() {
            String masked = DataMaskingUtil.maskIpAddress("1.2.3.4");
            assertThat(masked).startsWith("1.");
        }

        @Test
        @DisplayName("Should mask IPv6 address")
        void shouldMaskIpv6Address() {
            String masked = DataMaskingUtil.maskIpAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
            assertThat(masked).startsWith("2001:");
            assertThat(masked).contains("****");
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            assertThat(DataMaskingUtil.maskIpAddress(null)).isNull();
        }

        @Test
        @DisplayName("Should return empty for empty input")
        void shouldReturnEmptyForEmptyInput() {
            assertThat(DataMaskingUtil.maskIpAddress("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("maskTransaction tests")
    class MaskTransactionTests {

        @Test
        @DisplayName("Should mask all sensitive fields in transaction")
        void shouldMaskAllSensitiveFields() {
            TransactionModel original = createTestTransaction();

            TransactionModel masked = DataMaskingUtil.maskTransaction(original);

            // Verify sensitive fields are masked
            assertThat(masked.accountHolderName).isNotEqualTo(original.accountHolderName);
            assertThat(masked.mobileNumber).isNotEqualTo(original.mobileNumber);
            assertThat(masked.senderAccount).isNotEqualTo(original.senderAccount);
            assertThat(masked.receiverAccount).isNotEqualTo(original.receiverAccount);
            assertThat(masked.ipAddress).isNotEqualTo(original.ipAddress);

            // Verify specific masking patterns
            assertThat(masked.accountHolderName).isEqualTo("J*** D**");
            assertThat(masked.mobileNumber).isEqualTo("98****3210");
            assertThat(masked.senderAccount).isEqualTo("1234****9012");
            assertThat(masked.receiverAccount).isEqualTo("9876****1098");
        }

        @Test
        @DisplayName("Should preserve non-sensitive fields")
        void shouldPreserveNonSensitiveFields() {
            TransactionModel original = createTestTransaction();

            TransactionModel masked = DataMaskingUtil.maskTransaction(original);

            // Verify non-sensitive fields are preserved
            assertThat(masked.transactionId).isEqualTo(original.transactionId);
            assertThat(masked.amount).isEqualTo(original.amount);
            assertThat(masked.balance).isEqualTo(original.balance);
            assertThat(masked.riskLevel).isEqualTo(original.riskLevel);
            assertThat(masked.riskScore).isEqualTo(original.riskScore);
            assertThat(masked.isFraud).isEqualTo(original.isFraud);
            assertThat(masked.fraudReason).isEqualTo(original.fraudReason);
            assertThat(masked.location).isEqualTo(original.location);
            assertThat(masked.merchantCategory).isEqualTo(original.merchantCategory);
            assertThat(masked.transactionMode).isEqualTo(original.transactionMode);
            assertThat(masked.bankName).isEqualTo(original.bankName);
            assertThat(masked.timestamp).isEqualTo(original.timestamp);
        }

        @Test
        @DisplayName("Should return null for null transaction")
        void shouldReturnNullForNullTransaction() {
            assertThat(DataMaskingUtil.maskTransaction(null)).isNull();
        }

        @Test
        @DisplayName("Should handle transaction with null sensitive fields")
        void shouldHandleNullSensitiveFields() {
            TransactionModel original = new TransactionModel();
            original.transactionId = "txn-001";
            original.accountHolderName = null;
            original.mobileNumber = null;
            original.senderAccount = null;
            original.receiverAccount = null;
            original.ipAddress = null;

            TransactionModel masked = DataMaskingUtil.maskTransaction(original);

            assertThat(masked.transactionId).isEqualTo("txn-001");
            assertThat(masked.accountHolderName).isNull();
            assertThat(masked.mobileNumber).isNull();
            assertThat(masked.senderAccount).isNull();
            assertThat(masked.receiverAccount).isNull();
            assertThat(masked.ipAddress).isNull();
        }
    }

    @Nested
    @DisplayName("maskTransactions tests")
    class MaskTransactionsTests {

        @Test
        @DisplayName("Should mask all transactions in list")
        void shouldMaskAllTransactions() {
            List<TransactionModel> originals = Arrays.asList(
                    createTestTransaction("txn-001", "Alice Smith"),
                    createTestTransaction("txn-002", "Bob Jones")
            );

            List<TransactionModel> masked = DataMaskingUtil.maskTransactions(originals);

            assertThat(masked).hasSize(2);
            assertThat(masked.get(0).transactionId).isEqualTo("txn-001");
            assertThat(masked.get(0).accountHolderName).isEqualTo("A**** S****");
            assertThat(masked.get(1).transactionId).isEqualTo("txn-002");
            assertThat(masked.get(1).accountHolderName).isEqualTo("B** J****");
        }

        @Test
        @DisplayName("Should return null for null list")
        void shouldReturnNullForNullList() {
            assertThat(DataMaskingUtil.maskTransactions(null)).isNull();
        }

        @Test
        @DisplayName("Should handle empty list")
        void shouldHandleEmptyList() {
            List<TransactionModel> masked = DataMaskingUtil.maskTransactions(Collections.emptyList());
            assertThat(masked).isEmpty();
        }
    }

    // Helper methods

    private TransactionModel createTestTransaction() {
        return createTestTransaction("txn-001", "John Doe");
    }

    private TransactionModel createTestTransaction(String txnId, String name) {
        TransactionModel tx = new TransactionModel();
        tx.transactionId = txnId;
        tx.utrNumber = "UTR123456";
        tx.timestamp = LocalDateTime.now();
        tx.accountHolderName = name;
        tx.mobileNumber = "9876543210";
        tx.senderAccount = "123456789012";
        tx.receiverAccount = "987654321098";
        tx.bankName = "Test Bank";
        tx.accountAgeDays = 365;
        tx.type = "DEBIT";
        tx.amount = 150000.0;
        tx.balance = 500000.0;
        tx.currency = "INR";
        tx.merchantCategory = "Electronics";
        tx.merchantId = "MERCH001";
        tx.transactionMode = "ONLINE";
        tx.location = "Mumbai";
        tx.previousLocation = "Delhi";
        tx.isNewLocation = false;
        tx.distanceFromLastTxnKm = 1400.0;
        tx.device = "iPhone 14";
        tx.isNewDevice = false;
        tx.ipAddress = "192.168.1.100";
        tx.isVpnOrProxy = true;
        tx.ipCountry = "IN";
        tx.ipMatchesLocation = true;
        tx.ipRiskTag = "VPN";
        tx.txnCountLastHour = 5;
        tx.txnCountLast24Hours = 20;
        tx.avgTxnAmount30Days = 25000.0;
        tx.isInternational = false;
        tx.isFirstTimeReceiver = false;
        tx.isFraud = true;
        tx.fraudReason = "R01:HighAmount | R08:VPNDetected";
        tx.riskScore = 7.5;
        tx.riskLevel = "HIGH";
        tx.mlFraudProbability = 0.85;
        return tx;
    }
}
