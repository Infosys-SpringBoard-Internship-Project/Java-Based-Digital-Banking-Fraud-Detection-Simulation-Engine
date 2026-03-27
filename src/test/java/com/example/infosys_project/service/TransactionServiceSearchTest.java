package com.example.infosys_project.service;

import com.example.infosys_project.model.TransactionModel;
import com.example.infosys_project.repository.FraudAlertRepository;
import com.example.infosys_project.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TransactionService} search functionality.
 * Tests edge cases, filters, pagination, and query handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Search Tests")
class TransactionServiceSearchTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EmailAlertService emailAlertService;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @InjectMocks
    private TransactionService transactionService;

    private List<TransactionModel> testTransactions;

    @BeforeEach
    void setUp() {
        testTransactions = createTestTransactions();
    }

    private List<TransactionModel> createTestTransactions() {
        // Transaction 1: High risk fraud from Mumbai
        TransactionModel tx1 = new TransactionModel();
        tx1.transactionId = "txn-001";
        tx1.accountHolderName = "John Doe";
        tx1.senderAccount = "ACC123456";
        tx1.receiverAccount = "ACC789012";
        tx1.amount = 150000.0;
        tx1.riskLevel = "HIGH";
        tx1.riskScore = 8.5;
        tx1.isFraud = true;
        tx1.fraudReason = "R-01:HighAmount | R-05:VPNDetected";
        tx1.location = "Mumbai, India";
        tx1.merchantCategory = "Electronics";
        tx1.transactionMode = "ONLINE";
        tx1.ipRiskTag = "VPN";
        tx1.timestamp = LocalDateTime.of(2024, 1, 15, 10, 30);

        // Transaction 2: Medium risk from Delhi
        TransactionModel tx2 = new TransactionModel();
        tx2.transactionId = "txn-002";
        tx2.accountHolderName = "Jane Smith";
        tx2.senderAccount = "ACC555555";
        tx2.receiverAccount = "ACC666666";
        tx2.amount = 50000.0;
        tx2.riskLevel = "MEDIUM";
        tx2.riskScore = 4.5;
        tx2.isFraud = false;
        tx2.fraudReason = "R-03:NewLocation";
        tx2.location = "Delhi, India";
        tx2.merchantCategory = "Shopping";
        tx2.transactionMode = "ONLINE";
        tx2.ipRiskTag = "CLEAN";
        tx2.timestamp = LocalDateTime.of(2024, 1, 20, 14, 45);

        // Transaction 3: Critical fraud with TOR from unknown location
        TransactionModel tx3 = new TransactionModel();
        tx3.transactionId = "txn-003";
        tx3.accountHolderName = "Bob Johnson";
        tx3.senderAccount = "ACC777777";
        tx3.receiverAccount = "ACC888888";
        tx3.amount = 500000.0;
        tx3.riskLevel = "CRITICAL";
        tx3.riskScore = 9.8;
        tx3.isFraud = true;
        tx3.fraudReason = "R-01:HighAmount | R-07:TORDetected | R-ML:MLFlagged";
        tx3.location = "Unknown";
        tx3.merchantCategory = "Cryptocurrency";
        tx3.transactionMode = "ONLINE";
        tx3.ipRiskTag = "TOR";
        tx3.timestamp = LocalDateTime.of(2024, 1, 25, 22, 10);

        // Transaction 4: Low risk clean transaction
        TransactionModel tx4 = new TransactionModel();
        tx4.transactionId = "txn-004";
        tx4.accountHolderName = "Alice Brown";
        tx4.senderAccount = "ACC111111";
        tx4.receiverAccount = "ACC222222";
        tx4.amount = 5000.0;
        tx4.riskLevel = "LOW";
        tx4.riskScore = 1.2;
        tx4.isFraud = false;
        tx4.fraudReason = "None";
        tx4.location = "Bangalore, India";
        tx4.merchantCategory = "Groceries";
        tx4.transactionMode = "POS";
        tx4.ipRiskTag = "CLEAN";
        tx4.timestamp = LocalDateTime.of(2024, 2, 1, 9, 15);

        // Transaction 5: High risk with PROXY
        TransactionModel tx5 = new TransactionModel();
        tx5.transactionId = "txn-005";
        tx5.accountHolderName = "Charlie Davis";
        tx5.senderAccount = "ACC333333";
        tx5.receiverAccount = "ACC444444";
        tx5.amount = 200000.0;
        tx5.riskLevel = "HIGH";
        tx5.riskScore = 7.0;
        tx5.isFraud = true;
        tx5.fraudReason = "R-01:HighAmount | R-06:ProxyDetected";
        tx5.location = "Hyderabad, India";
        tx5.merchantCategory = "Travel";
        tx5.transactionMode = "ONLINE";
        tx5.ipRiskTag = "PROXY";
        tx5.timestamp = LocalDateTime.of(2024, 2, 5, 18, 30);

        return Arrays.asList(tx1, tx2, tx3, tx4, tx5);
    }

    @Nested
    @DisplayName("Basic Search Tests")
    class BasicSearchTests {

        @Test
        @DisplayName("Should return all transactions when no filters applied")
        void shouldReturnAllTransactionsWithNoFilters() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(5);
            assertThat(result.get("total")).isEqualTo(5);
            assertThat(result.get("page")).isEqualTo(0);
            assertThat(result.get("totalPages")).isEqualTo(1);
        }

        @Test
        @DisplayName("Should search by account holder name")
        void shouldSearchByAccountHolderName() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When
            Map<String, Object> result = transactionService.searchTransactions(
                    "john", null, null, null, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(2); // John Doe and Bob Johnson
            assertThat(items).extracting(t -> t.accountHolderName)
                    .containsAnyOf("John Doe", "Bob Johnson");
        }

        @Test
        @DisplayName("Should search by transaction ID")
        void shouldSearchByTransactionId() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When
            Map<String, Object> result = transactionService.searchTransactions(
                    "txn-003", null, null, null, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(1);
            assertThat(items.get(0).transactionId).isEqualTo("txn-003");
        }

        @Test
        @DisplayName("Should search by account number")
        void shouldSearchByAccountNumber() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - search for sender or receiver account
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", "ACC777777", "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(1);
            assertThat(items.get(0).senderAccount).isEqualTo("ACC777777");
        }

        @Test
        @DisplayName("Should search by location")
        void shouldSearchByLocation() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When
            Map<String, Object> result = transactionService.searchTransactions(
                    "mumbai", null, null, null, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(1);
            assertThat(items.get(0).location).containsIgnoringCase("Mumbai");
        }

        @Test
        @DisplayName("Should search by merchant category")
        void shouldSearchByMerchantCategory() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When
            Map<String, Object> result = transactionService.searchTransactions(
                    "crypto", null, null, null, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(1);
            assertThat(items.get(0).merchantCategory).containsIgnoringCase("Cryptocurrency");
        }
    }

    @Nested
    @DisplayName("Filter Tests")
    class FilterTests {

        @Test
        @DisplayName("Should filter by fraud status - fraud only")
        void shouldFilterByFraudStatus() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "fraud", "all", "all", null, "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(3); // tx1, tx3, tx5
            assertThat(items).allMatch(t -> t.isFraud);
        }

        @Test
        @DisplayName("Should filter by fraud status - non-fraud only")
        void shouldFilterByNonFraudStatus() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "non_fraud", "all", "all", null, "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(2); // tx2, tx4
            assertThat(items).noneMatch(t -> t.isFraud);
        }

        @Test
        @DisplayName("Should filter by risk level")
        void shouldFilterByRiskLevel() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "HIGH", "all", null, "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(2); // tx1, tx5
            assertThat(items).allMatch(t -> "HIGH".equals(t.riskLevel));
        }

        @Test
        @DisplayName("Should filter by IP risk tag")
        void shouldFilterByIpRiskTag() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", null, "TOR", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(1);
            assertThat(items.get(0).ipRiskTag).isEqualTo("TOR");
        }

        @Test
        @DisplayName("Should filter by fraud type")
        void shouldFilterByFraudType() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - search for transactions with VPN detected
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "vpndetected", null, "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(1);
            assertThat(items.get(0).fraudReason).containsIgnoringCase("VPNDetected");
        }

        @Test
        @DisplayName("Should filter by amount range")
        void shouldFilterByAmountRange() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - find transactions between 50k and 200k
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, 50000.0, 200000.0, "all", "all", "all", null, "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(3); // tx1 (150k), tx2 (50k), tx5 (200k)
            assertThat(items).allMatch(t -> t.amount >= 50000.0 && t.amount <= 200000.0);
        }

        @Test
        @DisplayName("Should filter by date range")
        void shouldFilterByDateRange() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - find transactions in January 2024
            Map<String, Object> result = transactionService.searchTransactions(
                    null, "2024-01-01", "2024-01-31", null, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(3); // tx1, tx2, tx3
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty repository")
        void shouldHandleEmptyRepository() {
            // Given
            when(transactionRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).isEmpty();
            assertThat(result.get("total")).isEqualTo(0);
            assertThat(result.get("totalPages")).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle case-insensitive search")
        void shouldHandleCaseInsensitiveSearch() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - search with UPPERCASE
            Map<String, Object> result = transactionService.searchTransactions(
                    "JOHN DOE", null, null, null, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then - should match "John Doe"
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(1);
            assertThat(items.get(0).accountHolderName).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("Should handle null/empty query string")
        void shouldHandleNullQuery() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - null query
            Map<String, Object> result1 = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", null, "all", 0, 50
            );

            // When - empty query
            Map<String, Object> result2 = transactionService.searchTransactions(
                    "", null, null, null, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then - both should return all transactions
            @SuppressWarnings("unchecked")
            List<TransactionModel> items1 = (List<TransactionModel>) result1.get("items");
            @SuppressWarnings("unchecked")
            List<TransactionModel> items2 = (List<TransactionModel>) result2.get("items");
            
            assertThat(items1).hasSize(5);
            assertThat(items2).hasSize(5);
        }

        @Test
        @DisplayName("Should handle invalid date format gracefully")
        void shouldHandleInvalidDateFormat() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - invalid date formats should be ignored
            Map<String, Object> result = transactionService.searchTransactions(
                    null, "invalid-date", "also-invalid", null, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then - should return all transactions (invalid dates ignored)
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(5);
        }

        @Test
        @DisplayName("Should handle negative amount filters")
        void shouldHandleNegativeAmounts() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - negative amounts (edge case)
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, -100.0, -1.0, "all", "all", "all", null, "all", 0, 50
            );

            // Then - no transactions match negative amounts
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).isEmpty();
        }

        @Test
        @DisplayName("Should handle very large amount filters")
        void shouldHandleVeryLargeAmounts() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - amounts larger than any transaction
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, 1000000.0, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then - no transactions match
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).isEmpty();
        }
    }

    @Nested
    @DisplayName("Pagination Tests")
    class PaginationTests {

        @Test
        @DisplayName("Should paginate results correctly")
        void shouldPaginateResults() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - page 0, size 2
            Map<String, Object> page1 = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", null, "all", 0, 2
            );

            // When - page 1, size 2
            Map<String, Object> page2 = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", null, "all", 1, 2
            );

            // Then
            @SuppressWarnings("unchecked")
            List<TransactionModel> page1Items = (List<TransactionModel>) page1.get("items");
            @SuppressWarnings("unchecked")
            List<TransactionModel> page2Items = (List<TransactionModel>) page2.get("items");
            
            assertThat(page1Items).hasSize(2);
            assertThat(page2Items).hasSize(2);
            assertThat(page1.get("totalPages")).isEqualTo(3);
            assertThat(page2.get("totalPages")).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle page size of 0")
        void shouldHandlePageSizeZero() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - size 0 should default to 50
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", null, "all", 0, 0
            );

            // Then
            assertThat(result.get("size")).isEqualTo(50);
        }

        @Test
        @DisplayName("Should cap page size at 500")
        void shouldCapPageSize() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - request size > 500
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", null, "all", 0, 1000
            );

            // Then - capped at 500
            assertThat(result.get("size")).isEqualTo(500);
        }

        @Test
        @DisplayName("Should handle page number beyond total pages")
        void shouldHandlePageBeyondTotal() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - request page 999
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", null, "all", 999, 50
            );

            // Then - should return empty items
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).isEmpty();
            assertThat(result.get("total")).isEqualTo(5);
        }

        @Test
        @DisplayName("Should handle negative page number")
        void shouldHandleNegativePageNumber() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - negative page defaults to 0
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", null, "all", -5, 50
            );

            // Then
            assertThat(result.get("page")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Combined Filter Tests")
    class CombinedFilterTests {

        @Test
        @DisplayName("Should combine multiple filters correctly")
        void shouldCombineMultipleFilters() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - fraud + HIGH risk + amount > 100k
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, 100000.0, null, "fraud", "HIGH", "all", null, "all", 0, 50
            );

            // Then - should match tx1 and tx5
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(2);
            assertThat(items).allMatch(t -> t.isFraud && "HIGH".equals(t.riskLevel) && t.amount >= 100000.0);
        }

        @Test
        @DisplayName("Should combine search query with filters")
        void shouldCombineSearchQueryWithFilters() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When - search for "Mumbai" + fraud + date range
            Map<String, Object> result = transactionService.searchTransactions(
                    "mumbai", "2024-01-01", "2024-01-31", null, null, "fraud", "all", "all", null, "all", 0, 50
            );

            // Then - should match tx1 only
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(1);
            assertThat(items.get(0).transactionId).isEqualTo("txn-001");
        }
    }

    @Nested
    @DisplayName("Sorting Tests")
    class SortingTests {

        @Test
        @DisplayName("Should sort by timestamp descending (most recent first)")
        void shouldSortByTimestampDescending() {
            // Given
            when(transactionRepository.findAll()).thenReturn(testTransactions);

            // When
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then - most recent first
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items.get(0).transactionId).isEqualTo("txn-005"); // Feb 5
            assertThat(items.get(items.size() - 1).transactionId).isEqualTo("txn-001"); // Jan 15
        }

        @Test
        @DisplayName("Should handle null timestamps in sorting without throwing exception")
        void shouldHandleNullTimestampsInSorting() {
            // Given - add transaction with null timestamp
            TransactionModel txNull = new TransactionModel();
            txNull.transactionId = "txn-null-time";
            txNull.accountHolderName = "Null Time User";
            txNull.amount = 10000.0;
            txNull.riskLevel = "LOW";
            txNull.riskScore = 1.0;
            txNull.isFraud = false;
            txNull.fraudReason = "None";
            txNull.timestamp = null;

            List<TransactionModel> txsWithNull = Arrays.asList(
                    testTransactions.get(0),
                    txNull,
                    testTransactions.get(1)
            );

            when(transactionRepository.findAll()).thenReturn(txsWithNull);

            // When
            Map<String, Object> result = transactionService.searchTransactions(
                    null, null, null, null, null, "all", "all", "all", null, "all", 0, 50
            );

            // Then - should not throw exception, all items returned
            @SuppressWarnings("unchecked")
            List<TransactionModel> items = (List<TransactionModel>) result.get("items");
            assertThat(items).hasSize(3);
            // Verify the null timestamp transaction is in the result
            assertThat(items.stream().anyMatch(tx -> "txn-null-time".equals(tx.transactionId))).isTrue();
        }
    }
}
