// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omar.beautycenter.auth.dto.LoginRequest;
import com.omar.beautycenter.closing.CashAdjustmentType;
import com.omar.beautycenter.closing.dto.CloseDailyRequest;
import com.omar.beautycenter.closing.dto.CreateCashAdjustmentRequest;
import com.omar.beautycenter.closing.dto.ReopenDailyRequest;
import com.omar.beautycenter.expense.dto.CreateExpenseRequest;
import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.sales.dto.CreateSimpleOrderRequest;
import com.omar.beautycenter.user.User;
import com.omar.beautycenter.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardClosingIntegrationTest {

    // TEST ONLY - synthetic password.
    private static final String PASSWORD = "test-only-strong-password";
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 7, 20);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserRepository userRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM hair_stock_movements");
        jdbcTemplate.update("DELETE FROM hair_product_stocks");
        jdbcTemplate.update("DELETE FROM daily_closing_payment_totals");
        jdbcTemplate.update("DELETE FROM daily_closing_expense_totals");
        jdbcTemplate.update("DELETE FROM daily_closings");
        jdbcTemplate.update("DELETE FROM cash_adjustments");
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM invoice_refund_payments");
        jdbcTemplate.update("DELETE FROM invoice_refunds");
        jdbcTemplate.update("DELETE FROM invoice_payments");
        jdbcTemplate.update("DELETE FROM invoice_items");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM expenses");
        jdbcTemplate.update("DELETE FROM employee_attendances");
        jdbcTemplate.update("DELETE FROM employee_transactions");
        jdbcTemplate.update("DELETE FROM discounts");
        jdbcTemplate.update("DELETE FROM price_history");
        jdbcTemplate.update("DELETE FROM catalog_items");
        jdbcTemplate.update("DELETE FROM catalog_categories");
        jdbcTemplate.update("DELETE FROM employees");
        userRepository.deleteAll();
    }

    @Test
    void dashboardAndClosingUseSnapshotsCashRulesAndClosedDateGuard() throws Exception {
        String ownerToken = createUserAndLogin("phase6-owner", Role.OWNER);
        String managerToken = createUserAndLogin("phase6-admin", Role.ADMIN);
        String receptionistToken = createUserAndLogin("phase6-receptionist", Role.RECEPTIONIST);

        mockMvc.perform(get("/api/v1/dashboard")
                        .param("filter", "CUSTOM")
                        .param("fromDate", BUSINESS_DATE.minusDays(1).toString())
                        .param("toDate", BUSINESS_DATE.minusDays(1).toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossSales").value(0.00))
                .andExpect(jsonPath("$.netProfit").value(0.00))
                .andExpect(jsonPath("$.expectedCashBalance").value(0.00));

        Fixture fixture = createFinancialFixture();

        mockMvc.perform(get("/api/v1/dashboard")
                        .param("filter", "CUSTOM")
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossSales").value(1000.00))
                .andExpect(jsonPath("$.discounts").value(100.00))
                .andExpect(jsonPath("$.refunds").value(100.00))
                .andExpect(jsonPath("$.netSales").value(800.00))
                .andExpect(jsonPath("$.completedInvoiceCount").value(1))
                .andExpect(jsonPath("$.expenses").value(100.00))
                .andExpect(jsonPath("$.netSalesAfterExpenses").value(700.00))
                .andExpect(jsonPath("$.dailyWages").value(40.00))
                .andExpect(jsonPath("$.bonuses").value(10.00))
                .andExpect(jsonPath("$.commissions").value(5.00))
                .andExpect(jsonPath("$.employeeCost").value(55.00))
                .andExpect(jsonPath("$.advances").value(20.00))
                .andExpect(jsonPath("$.employeePayments").value(15.00))
                .andExpect(jsonPath("$.costOfSoldItemsAndHair").value(200.00))
                .andExpect(jsonPath("$.netProfit").value(445.00))
                .andExpect(jsonPath("$.cashPaymentsReceived").value(400.00))
                .andExpect(jsonPath("$.cashExpenses").value(30.00))
                .andExpect(jsonPath("$.cashEmployeePayments").value(25.00))
                .andExpect(jsonPath("$.cashRefunds").value(40.00))
                .andExpect(jsonPath("$.cashAdjustments").value(25.00))
                .andExpect(jsonPath("$.expectedCashBalance").value(330.00))
                .andExpect(jsonPath("$.paymentTotalsByMethod.length()").value(2))
                .andExpect(jsonPath("$.expenseTotalsByCategory[0].amount").value(100.00))
                .andExpect(jsonPath("$.topServicesAndProducts[0].itemCode").value("PHASE6_PRODUCT"))
                .andExpect(jsonPath("$.topPerformingEmployees[0].employeeName").value("Phase 6 Employee"))
                .andExpect(jsonPath("$.dailySalesTrend[0].netSales").value(800.00));

        mockMvc.perform(get("/api/v1/reports/financial")
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netSales").value(800.00))
                .andExpect(jsonPath("$.expenses").value(100.00))
                .andExpect(jsonPath("$.netSalesAfterExpenses").value(700.00));

        mockMvc.perform(post("/api/v1/daily-closings/{date}/close", BUSINESS_DATE)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CloseDailyRequest(
                                new BigDecimal("350.00"), "Register counted"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.expectedCash").value(330.00))
                .andExpect(jsonPath("$.actualCash").value(350.00))
                .andExpect(jsonPath("$.cashDifference").value(20.00))
                .andExpect(jsonPath("$.paymentMethodBreakdown.length()").value(2));

        mockMvc.perform(post("/api/v1/expenses")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExpenseRequest(
                                fixture.expenseCategoryId(), new BigDecimal("1.00"), BUSINESS_DATE,
                                fixture.cashMethodId(), null, "Blocked expense", null, null))))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/cash-adjustments")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCashAdjustmentRequest(
                                BUSINESS_DATE, CashAdjustmentType.CASH_IN,
                                new BigDecimal("1.00"), "Blocked adjustment"))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/daily-closings/{date}/reopen", BUSINESS_DATE)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReopenDailyRequest("Staff attempt"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/daily-closings/{date}/reopen", BUSINESS_DATE)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReopenDailyRequest("Admin attempt"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/daily-closings/{date}/reopen", BUSINESS_DATE)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReopenDailyRequest("Approved correction"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"))
                .andExpect(jsonPath("$.reopeningReason").value("Approved correction"));

        mockMvc.perform(get("/api/v1/reports/daily-closing/{date}", BUSINESS_DATE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"))
                .andExpect(jsonPath("$.closingVersion").value(1))
                .andExpect(jsonPath("$.expectedCash").value(330.00));

        mockMvc.perform(post("/api/v1/invoices/simple-orders")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSimpleOrderRequest(
                                "Closing correction customer", null, BUSINESS_DATE, "Correction order",
                                new BigDecimal("50.00"), fixture.cashMethodId(), null, null, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/expenses")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExpenseRequest(
                                fixture.expenseCategoryId(), new BigDecimal("20.00"), BUSINESS_DATE,
                                fixture.cashMethodId(), null, "Correction expense", null, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/daily-closings/{date}/close", BUSINESS_DATE)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CloseDailyRequest(
                                new BigDecimal("380.00"), "Reclosed after corrections"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closingVersion").value(2))
                .andExpect(jsonPath("$.expectedCash").value(360.00));

        mockMvc.perform(post("/api/v1/daily-closings/{date}/close", BUSINESS_DATE)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CloseDailyRequest(
                                new BigDecimal("380.00"), "Duplicate close"))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/reports/daily-closing/{date}", BUSINESS_DATE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closingVersion").value(2))
                .andExpect(jsonPath("$.expectedCash").value(360.00))
                .andExpect(jsonPath("$.cashDifference").value(20.00));

        mockMvc.perform(get("/api/v1/daily-closings/{date}/versions", BUSINESS_DATE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].closingVersion").value(2))
                .andExpect(jsonPath("$[0].expectedCash").value(360.00))
                .andExpect(jsonPath("$[1].closingVersion").value(1))
                .andExpect(jsonPath("$[1].status").value("REOPENED"))
                .andExpect(jsonPath("$[1].expectedCash").value(330.00));

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("action", "DAILY_CLOSING_COMPLETED")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        assertThat(countAudit("LOGIN_SUCCESS")).isGreaterThanOrEqualTo(3);
        assertThat(countAudit("DAILY_CLOSING_REOPENED")).isEqualTo(1);
    }

    private Fixture createFinancialFixture() {
        long cashId = id("SELECT id FROM payment_methods WHERE code = 'CASH'");
        long visaId = id("SELECT id FROM payment_methods WHERE code = 'VISA'");
        long expenseCategoryId = id("SELECT id FROM expense_categories WHERE name_english = 'Other'");

        jdbcTemplate.update("""
                INSERT INTO catalog_categories
                    (name_arabic, name_english, active, version, created_at, updated_at, created_by, updated_by)
                VALUES ('Phase 6 Arabic', 'Phase 6 Products', TRUE, 0, CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """);
        long catalogCategoryId = id(
                "SELECT id FROM catalog_categories WHERE name_english = 'Phase 6 Products'");
        jdbcTemplate.update("""
                INSERT INTO catalog_items
                    (code, name_arabic, name_english, item_type, category_id,
                     current_selling_price, current_cost, active, version,
                     created_at, updated_at, created_by, updated_by)
                VALUES ('PHASE6_PRODUCT', 'Phase 6 Product Arabic', 'Phase 6 Product', 'PRODUCT', ?,
                        500.00, 100.00, TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """, catalogCategoryId);
        long itemId = id("SELECT id FROM catalog_items WHERE code = 'PHASE6_PRODUCT'");
        jdbcTemplate.update("""
                INSERT INTO discounts
                    (name, discount_type, discount_value, start_date, end_date, active, catalog_item_id,
                     version, created_at, updated_at, created_by, updated_by)
                VALUES ('Phase 6 Discount', 'PERCENTAGE', 10.00, ?, ?, TRUE, ?, 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """, BUSINESS_DATE.minusDays(1), BUSINESS_DATE.plusDays(1), itemId);
        long discountId = id("SELECT id FROM discounts WHERE name = 'Phase 6 Discount'");

        jdbcTemplate.update("""
                INSERT INTO employees
                    (name, position, default_daily_wage, active, version,
                     created_at, updated_at, created_by, updated_by)
                VALUES ('Phase 6 Employee', 'Stylist', 40.00, TRUE, 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """);
        long employeeId = id("SELECT id FROM employees WHERE name = 'Phase 6 Employee'");

        jdbcTemplate.update("""
                INSERT INTO invoices
                    (invoice_number, business_date, status, subtotal, discount_total, final_total,
                     refunded_total, completed_by, completed_at, version,
                     created_at, updated_at, created_by, updated_by)
                VALUES ('INV-PHASE6-001', ?, 'PARTIALLY_REFUNDED', 1000.00, 100.00, 900.00,
                        100.00, 'fixture', CURRENT_TIMESTAMP, 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """, BUSINESS_DATE);
        long invoiceId = id("SELECT id FROM invoices WHERE invoice_number = 'INV-PHASE6-001'");
        jdbcTemplate.update("""
                INSERT INTO invoice_items
                    (invoice_id, catalog_item_id, item_code_snapshot, item_name_arabic_snapshot,
                     item_name_english_snapshot, item_type_snapshot, quantity,
                     unit_original_selling_price, unit_cost_snapshot, installation_fee_snapshot,
                     discount_id, discount_name_snapshot, discount_type_snapshot, discount_value_snapshot,
                     discount_amount, line_subtotal, final_line_total, employee_id, employee_name_snapshot,
                     version, created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, 'PHASE6_PRODUCT', 'Phase 6 Product Arabic', 'Phase 6 Product', 'PRODUCT',
                        2.000, 500.00, 100.00, 0.00, ?, 'Phase 6 Discount', 'PERCENTAGE', 10.00,
                        100.00, 1000.00, 900.00, ?, 'Phase 6 Employee', 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """, invoiceId, itemId, discountId, employeeId);
        insertInvoicePayment(invoiceId, cashId, "CASH", "Cash", "CASH", "400.00");
        insertInvoicePayment(invoiceId, visaId, "VISA", "Visa", "CARD", "500.00");

        jdbcTemplate.update("""
                INSERT INTO invoice_refunds
                    (invoice_id, business_date, amount, reason, status, version,
                     created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, 100.00, 'Phase 6 refund', 'COMPLETED', 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """, invoiceId, BUSINESS_DATE);
        long refundId = id("SELECT id FROM invoice_refunds WHERE invoice_id = " + invoiceId);
        insertRefundPayment(refundId, cashId, "CASH", "Cash", "CASH", "40.00");
        insertRefundPayment(refundId, visaId, "VISA", "Visa", "CARD", "60.00");

        insertExpense(expenseCategoryId, cashId, "CASH", "Cash", "CASH", "30.00");
        insertExpense(expenseCategoryId, visaId, "VISA", "Visa", "CARD", "70.00");
        insertCancelledExpense(expenseCategoryId, cashId, "CASH", "Cash", "CASH", "50.00");
        insertCancelledInvoice();
        insertEmployeeTransaction(employeeId, "DAILY_WAGE", "40.00", null, null, null);
        insertEmployeeTransaction(employeeId, "BONUS", "10.00", cashId, "CASH", "CASH");
        insertEmployeeTransaction(employeeId, "COMMISSION", "5.00", null, null, null);
        insertEmployeeTransaction(employeeId, "ADVANCE", "20.00", visaId, "VISA", "CARD");
        insertEmployeeTransaction(employeeId, "PAYMENT", "15.00", cashId, "CASH", "CASH");

        insertCashAdjustment("CASH_IN", "30.00", "Cash float correction");
        insertCashAdjustment("CASH_OUT", "5.00", "Petty cash correction");
        return new Fixture(cashId, expenseCategoryId);
    }

    private void insertInvoicePayment(
            long invoiceId, long methodId, String code, String name, String type, String amount) {
        jdbcTemplate.update("""
                INSERT INTO invoice_payments
                    (invoice_id, payment_method_id, payment_method_code_snapshot,
                     payment_method_name_arabic_snapshot, payment_method_name_english_snapshot,
                     payment_method_type_snapshot, amount, version,
                     created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """, invoiceId, methodId, code, name, name, type, new BigDecimal(amount));
    }

    private void insertRefundPayment(
            long refundId, long methodId, String code, String name, String type, String amount) {
        jdbcTemplate.update("""
                INSERT INTO invoice_refund_payments
                    (refund_id, payment_method_id, payment_method_code_snapshot,
                     payment_method_name_arabic_snapshot, payment_method_name_english_snapshot,
                     payment_method_type_snapshot, amount,
                     created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """, refundId, methodId, code, name, name, type, new BigDecimal(amount));
    }

    private void insertExpense(
            long categoryId, long methodId, String code, String name, String type, String amount) {
        jdbcTemplate.update("""
                INSERT INTO expenses
                    (category_id, category_name_arabic, category_name_english, amount, business_date,
                     payment_method_id, payment_method_code, payment_method_name_arabic,
                     payment_method_name_english, payment_method_type, description, status, version,
                     created_at, updated_at, created_by, updated_by)
                VALUES (?, 'Other', 'Other', ?, ?, ?, ?, ?, ?, ?, 'Phase 6 expense', 'ACTIVE', 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """, categoryId, new BigDecimal(amount), BUSINESS_DATE, methodId, code, name, name, type);
    }

    private void insertCancelledExpense(
            long categoryId, long methodId, String code, String name, String type, String amount) {
        jdbcTemplate.update("""
                INSERT INTO expenses
                    (category_id, category_name_arabic, category_name_english, amount, business_date,
                     payment_method_id, payment_method_code, payment_method_name_arabic,
                     payment_method_name_english, payment_method_type, description, status,
                     cancelled_at, cancelled_by, cancellation_reason, version,
                     created_at, updated_at, created_by, updated_by)
                VALUES (?, 'Other', 'Other', ?, ?, ?, ?, ?, ?, ?, 'Cancelled fixture expense', 'CANCELLED',
                        CURRENT_TIMESTAMP, 'fixture', 'Cancelled for dashboard test', 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """, categoryId, new BigDecimal(amount), BUSINESS_DATE, methodId, code, name, name, type);
    }

    private void insertCancelledInvoice() {
        jdbcTemplate.update("""
                INSERT INTO invoices
                    (invoice_number, business_date, status, subtotal, discount_total, final_total,
                     refunded_total, cancelled_by, cancelled_at, cancellation_reason, version,
                     created_at, updated_at, created_by, updated_by)
                VALUES ('INV-PHASE6-CANCELLED', ?, 'CANCELLED', 500.00, 50.00, 450.00, 0.00,
                        'fixture', CURRENT_TIMESTAMP, 'Cancelled for dashboard test', 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """, BUSINESS_DATE);
    }

    private void insertEmployeeTransaction(
            long employeeId, String type, String amount, Long methodId, String methodCode, String methodType) {
        jdbcTemplate.update("""
                INSERT INTO employee_transactions
                    (employee_id, transaction_type, amount, business_date, payment_method,
                     payment_method_id, payment_method_type, status, version,
                     created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """, employeeId, type, new BigDecimal(amount), BUSINESS_DATE, methodCode, methodId, methodType);
    }

    private void insertCashAdjustment(String type, String amount, String reason) {
        jdbcTemplate.update("""
                INSERT INTO cash_adjustments
                    (business_date, adjustment_type, amount, reason, status, version,
                     created_at, updated_at, created_by, updated_by)
                VALUES (?, ?, ?, ?, 'ACTIVE', 0,
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'fixture', 'fixture')
                """, BUSINESS_DATE, type, new BigDecimal(amount), reason);
    }

    private String createUserAndLogin(String username, Role role) throws Exception {
        userRepository.saveAndFlush(new User(username, "Test " + role,
                passwordEncoder.encode(PASSWORD), true, Set.of(role)));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accessToken").asText();
    }

    private long id(String query) {
        return jdbcTemplate.queryForObject(query, Long.class);
    }

    private int countAudit(String action) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = ?", Integer.class, action);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record Fixture(long cashMethodId, long expenseCategoryId) { }
}
