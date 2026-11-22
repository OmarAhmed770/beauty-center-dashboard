// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omar.beautycenter.auth.dto.LoginRequest;
import com.omar.beautycenter.closing.BusinessDateWriteGuard;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.discount.DiscountType;
import com.omar.beautycenter.expense.dto.CreateExpenseRequest;
import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.sales.dto.CancelInvoiceRequest;
import com.omar.beautycenter.sales.dto.CreateDraftInvoiceRequest;
import com.omar.beautycenter.sales.dto.CreateInvoiceRefundRequest;
import com.omar.beautycenter.sales.dto.HairRefundReturnRequest;
import com.omar.beautycenter.sales.dto.CreateQuickOrderRequest;
import com.omar.beautycenter.sales.dto.CreateSimpleOrderRequest;
import com.omar.beautycenter.sales.dto.InvoicePaymentRequest;
import com.omar.beautycenter.sales.dto.SetInvoicePaymentsRequest;
import com.omar.beautycenter.sales.dto.UpsertInvoiceItemRequest;
import com.omar.beautycenter.sales.dto.QuickOrderLineRequest;
import com.omar.beautycenter.sales.dto.QuickOrderPaymentRequest;
import com.omar.beautycenter.user.SystemPermission;
import com.omar.beautycenter.user.User;
import com.omar.beautycenter.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(InvoiceIntegrationTest.ClosedDateTestConfiguration.class)
class InvoiceIntegrationTest {

    // TEST ONLY - synthetic password.
    private static final String PASSWORD = "test-only-strong-password";
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 7, 26);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestBusinessDateWriteGuard dateWriteGuard;

    @BeforeEach
    void cleanDatabase() {
        dateWriteGuard.clear();
        jdbcTemplate.update("DELETE FROM hair_stock_movements");
        jdbcTemplate.update("DELETE FROM hair_product_stocks");
        jdbcTemplate.update("DELETE FROM daily_closing_payment_totals");
        jdbcTemplate.update("DELETE FROM daily_closing_expense_totals");
        jdbcTemplate.update("DELETE FROM daily_closings");
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM invoice_refund_payments");
        jdbcTemplate.update("DELETE FROM invoice_refunds");
        jdbcTemplate.update("DELETE FROM invoice_payments");
        jdbcTemplate.update("DELETE FROM invoice_items");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM hair_price_override_audits");
        jdbcTemplate.update("DELETE FROM hair_configurations");
        jdbcTemplate.update("DELETE FROM discounts");
        jdbcTemplate.update("DELETE FROM price_history");
        jdbcTemplate.update("DELETE FROM catalog_items");
        jdbcTemplate.update("DELETE FROM catalog_categories");
        jdbcTemplate.update("DELETE FROM employee_attendances");
        jdbcTemplate.update("DELETE FROM employee_transactions");
        jdbcTemplate.update("DELETE FROM employees");
        userRepository.deleteAll();
    }

    @Test
    void completionRejectsNoItemsAndInvalidQuantities() throws Exception {
        String token = createUserAndLogin(
                "reception", Role.RECEPTIONIST, Set.of(SystemPermission.INVOICES_AND_RETURNS));
        long invoiceId = createDraft(token);

        complete(token, invoiceId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An invoice must contain at least one item."));

        long itemId = createCatalogItem("INVALID_QTY", "100.00", "40.00");
        addItem(token, invoiceId, itemRequest(itemId, "0.000", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(count("invoice_items")).isZero();
        assertThat(statusOf(invoiceId)).isEqualTo("DRAFT");
    }

    @Test
    void discountAndCatalogPriceSnapshotsRemainImmutableAtCompletion() throws Exception {
        String token = createUserAndLogin("admin", Role.ADMIN);
        long itemId = createCatalogItem("SNAPSHOT_SERVICE", "100.00", "30.00");
        long discountId = createDiscount(itemId, DiscountType.PERCENTAGE, "10.00");
        long invoiceId = createDraft(token);

        addItem(token, invoiceId, itemRequest(itemId, "2.000", discountId, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(200.00))
                .andExpect(jsonPath("$.discountTotal").value(20.00))
                .andExpect(jsonPath("$.finalTotal").value(180.00))
                .andExpect(jsonPath("$.items[0].unitOriginalSellingPrice").value(100.00))
                .andExpect(jsonPath("$.items[0].unitCostSnapshot").value(30.00));

        jdbcTemplate.update(
                "UPDATE catalog_items SET current_selling_price = 999.00, current_cost = 888.00 WHERE id = ?",
                itemId);
        setPayments(token, invoiceId, payment("CASH", "180.00", null))
                .andExpect(status().isOk());
        complete(token, invoiceId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.items[0].unitOriginalSellingPrice").value(100.00))
                .andExpect(jsonPath("$.items[0].unitCostSnapshot").value(30.00))
                .andExpect(jsonPath("$.items[0].discountType").value("PERCENTAGE"))
                .andExpect(jsonPath("$.items[0].discountValue").value(10.00));

        assertThat(countAudit("INVOICE_COMPLETED")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT current_selling_price FROM catalog_items WHERE id = ?",
                BigDecimal.class,
                itemId)).isEqualByComparingTo("999.00");
    }

    @Test
    void splitPaymentsMustMatchExactlyAndRollbackFailedCompletion() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long itemId = createCatalogItem("SPLIT_SERVICE", "1500.00", "500.00");
        long invoiceId = createDraftWithItem(token, itemId, "1.000");

        setPayments(
                        token,
                        invoiceId,
                        payment("CASH", "500.00", null),
                        payment("VISA", "900.00", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments.length()").value(2));
        complete(token, invoiceId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Payment total must equal the final invoice amount."));

        assertThat(statusOf(invoiceId)).isEqualTo("DRAFT");
        assertThat(countAudit("INVOICE_COMPLETED")).isZero();
        assertThat(count("invoice_items")).isEqualTo(1);
        assertThat(count("invoice_payments")).isEqualTo(2);

        setPayments(
                        token,
                        invoiceId,
                        payment("CASH", "500.00", null),
                        payment("VISA", "1000.00", null))
                .andExpect(status().isOk());
        complete(token, invoiceId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.payments[0].amount").value(500.00))
                .andExpect(jsonPath("$.payments[1].amount").value(1000.00));
        assertThat(countAudit("INVOICE_COMPLETED")).isEqualTo(1);
    }

    @Test
    void requiredPaymentReferenceIsEnforcedWithoutPartialReplacement() throws Exception {
        String token = createUserAndLogin(
                "reception", Role.RECEPTIONIST, Set.of(SystemPermission.INVOICES_AND_RETURNS));
        long invoiceId = createDraftWithItem(
                token,
                createCatalogItem("REFERENCE_SERVICE", "100.00", "20.00"),
                "1.000");
        setPayments(token, invoiceId, payment("CASH", "100.00", null))
                .andExpect(status().isOk());

        setPayments(token, invoiceId, payment("FAWRY", "100.00", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "A reference number is required for the selected payment method."));

        mockMvc.perform(get("/api/v1/invoices/{invoiceId}", invoiceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments.length()").value(1))
                .andExpect(jsonPath("$.payments[0].paymentMethodCode").value("CASH"));

        setPayments(token, invoiceId, payment("FAWRY", "100.00", "FW-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments[0].referenceNumber").value("FW-123"));
    }

    @Test
    void completedInvoiceIsImmutableAndCancellationPreservesFinancialRows() throws Exception {
        String ownerToken = createUserAndLogin("owner", Role.OWNER);
        long itemId = createCatalogItem("IMMUTABLE_SERVICE", "250.00", "50.00");
        long invoiceId = createDraftWithItem(ownerToken, itemId, "1.000");
        setPayments(ownerToken, invoiceId, payment("CASH", "250.00", null));
        JsonNode completed = readJson(complete(ownerToken, invoiceId)
                .andExpect(status().isOk())
                .andReturn());
        long invoiceItemId = completed.path("items").get(0).path("id").asLong();

        addItem(ownerToken, invoiceId, itemRequest(itemId, "1.000", null, null))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/invoices/{invoiceId}/items/{itemId}", invoiceId, invoiceItemId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                itemRequest(itemId, "2.000", null, null))))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/invoices/{invoiceId}/items/{itemId}", invoiceId, invoiceItemId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isConflict());

        String receptionistToken = createUserAndLogin("reception", Role.RECEPTIONIST);
        cancel(receptionistToken, invoiceId, "Not permitted")
                .andExpect(status().isForbidden());
        cancel(ownerToken, invoiceId, "Customer sale voided")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Customer sale voided"))
                .andExpect(jsonPath("$.cancelledBy").value("owner"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.payments.length()").value(1));

        assertThat(count("invoice_items")).isEqualTo(1);
        assertThat(count("invoice_payments")).isEqualTo(1);
        assertThat(countAudit("INVOICE_COMPLETED")).isEqualTo(1);
        assertThat(countAudit("INVOICE_CANCELLED")).isEqualTo(1);
    }

    @Test
    void partialAndFullRefundRecordsUpdateStatusAndPreserveOriginalSale() throws Exception {
        String token = createUserAndLogin("admin", Role.ADMIN);
        long invoiceId = createDraftWithItem(
                token,
                createCatalogItem("REFUND_SERVICE", "150.00", "60.00"),
                "1.000");
        setPayments(token, invoiceId, payment("CASH", "150.00", null));
        complete(token, invoiceId).andExpect(status().isOk());

        refund(token, invoiceId, "50.00", "Partial customer refund")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.payments[0].amount").value(50.00));
        mockMvc.perform(get("/api/v1/invoices/{invoiceId}", invoiceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.refundedTotal").value(50.00));

        refund(token, invoiceId, "100.00", "Remaining customer refund")
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/invoices/{invoiceId}", invoiceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.refundedTotal").value(150.00))
                .andExpect(jsonPath("$.refunds.length()").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.payments.length()").value(1));

        assertThat(count("invoice_refunds")).isEqualTo(2);
        assertThat(count("invoice_refund_payments")).isEqualTo(2);
        assertThat(countAudit("INVOICE_COMPLETED")).isEqualTo(1);
        assertThat(countAudit("INVOICE_REFUNDED")).isEqualTo(2);
    }

    @Test
    void refundDateCannotPrecedeOriginalInvoiceDate() throws Exception {
        String token = createUserAndLogin("refund.admin", Role.ADMIN);
        long invoiceId = createDraftWithItem(
                token,
                createCatalogItem("REFUND_DATE_SERVICE", "150.00", "60.00"),
                "1.000");
        setPayments(token, invoiceId, payment("CASH", "150.00", null));
        complete(token, invoiceId).andExpect(status().isOk());

        refund(token, invoiceId, BUSINESS_DATE.minusDays(1), "50.00", "Invalid refund date")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Refund business date cannot be before the invoice business date."));

        assertThat(count("invoice_refunds")).isZero();
        assertThat(countAudit("INVOICE_REFUNDED")).isZero();
        assertThat(statusOf(invoiceId)).isEqualTo("COMPLETED");
    }

    @Test
    void closedBusinessDatesRejectDraftCreationAndAllDraftChanges() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long itemId = createCatalogItem("CLOSED_SERVICE", "75.00", "20.00");
        long invoiceId = createDraftWithItem(token, itemId, "1.000");
        setPayments(token, invoiceId, payment("CASH", "75.00", null));
        dateWriteGuard.close(BUSINESS_DATE);

        mockMvc.perform(post("/api/v1/invoices")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Business date is closed."));
        addItem(token, invoiceId, itemRequest(itemId, "1.000", null, null))
                .andExpect(status().isConflict());
        complete(token, invoiceId)
                .andExpect(status().isConflict());

        assertThat(statusOf(invoiceId)).isEqualTo("DRAFT");
        assertThat(countAudit("INVOICE_COMPLETED")).isZero();
    }

    @Test
    void simpleOrdersCompleteForOwnerAdminAndReceptionist() throws Exception {
        for (Role role : List.of(Role.OWNER, Role.ADMIN, Role.RECEPTIONIST)) {
            String token = createUserAndLogin("simple-" + role.name().toLowerCase(), role);

            ResultActions result = simpleOrder(token, simpleOrderRequest(
                    "Manual " + role.name(), "125.00", paymentMethodId("CASH"), null, null))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.subtotal").value(125.00))
                    .andExpect(jsonPath("$.finalTotal").value(125.00))
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].manualItem").value(true))
                    .andExpect(jsonPath("$.items[0].itemNameArabic").value("Manual " + role.name()))
                    .andExpect(jsonPath("$.items[0].quantity").value(1.000))
                    .andExpect(jsonPath("$.items[0].unitOriginalSellingPrice").value(125.00))
                    .andExpect(jsonPath("$.items[0].finalLineTotal").value(125.00))
                    .andExpect(jsonPath("$.payments[0].amount").value(125.00));

            if (role == Role.RECEPTIONIST) {
                result.andExpect(jsonPath("$.items[0].unitCostSnapshot").doesNotExist());
            } else {
                result.andExpect(jsonPath("$.items[0].unitCostSnapshot").value(0.00));
            }
            JsonNode completed = readJson(result.andReturn());

            assertThat(completed.path("items").get(0).path("catalogItemId").isNull()).isTrue();
        }

        assertThat(count("invoices")).isEqualTo(3);
        assertThat(count("invoice_items")).isEqualTo(3);
        assertThat(count("invoice_payments")).isEqualTo(3);
        assertThat(countAudit("INVOICE_COMPLETED")).isEqualTo(3);
    }

    @Test
    void simpleOrdersRejectUnauthenticatedAndInvalidRequests() throws Exception {
        long cashMethodId = paymentMethodId("CASH");
        mockMvc.perform(post("/api/v1/invoices/simple-orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(simpleOrderRequest(
                                "Manual service", "50.00", cashMethodId, null, null))))
                .andExpect(status().isUnauthorized());

        String token = createUserAndLogin("simple-validation", Role.RECEPTIONIST);
        simpleOrder(token, simpleOrderRequest("   ", "50.00", cashMethodId, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        simpleOrder(token, simpleOrderRequest("Manual service", "0.00", cashMethodId, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        simpleOrder(token, simpleOrderRequest("Manual service", "-1.00", cashMethodId, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        simpleOrder(token, simpleOrderRequest("Manual service", "50.00", null, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        simpleOrder(token, new CreateSimpleOrderRequest(
                null, null, null, "Manual service", new BigDecimal("50.00"), cashMethodId,
                null, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        assertThat(count("invoices")).isZero();
    }

    @Test
    void simpleOrdersValidatePaymentMethodsEmployeesAndRequiredReferences() throws Exception {
        String token = createUserAndLogin("simple-masters", Role.ADMIN);
        long cashMethodId = paymentMethodId("CASH");

        jdbcTemplate.update("UPDATE payment_methods SET active = FALSE WHERE id = ?", cashMethodId);
        try {
            simpleOrder(token, simpleOrderRequest("Manual service", "50.00", cashMethodId, null, null))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(
                            "Inactive payment methods cannot be used for new records."));
        } finally {
            jdbcTemplate.update("UPDATE payment_methods SET active = TRUE WHERE id = ?", cashMethodId);
        }

        simpleOrder(token, simpleOrderRequest("Manual service", "50.00", cashMethodId, null, 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        long inactiveEmployeeId = createEmployee("Inactive simple employee", false);
        simpleOrder(token, simpleOrderRequest(
                "Manual service", "50.00", cashMethodId, null, inactiveEmployeeId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Only active employees may be assigned to new invoice items."));

        long fawryMethodId = paymentMethodId("FAWRY");
        simpleOrder(token, simpleOrderRequest("Manual service", "50.00", fawryMethodId, null, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "A reference number is required for the selected payment method."));
        simpleOrder(token, simpleOrderRequest("Manual service", "50.00", fawryMethodId, "FW-123", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payments[0].referenceNumber").value("FW-123"));

        assertThat(count("invoices")).isEqualTo(1);
    }

    @Test
    void simpleOrdersRespectClosedDatesAndRollbackWhenCompletionFails() throws Exception {
        String token = createUserAndLogin("simple-closed", Role.OWNER);
        long cashMethodId = paymentMethodId("CASH");
        dateWriteGuard.close(BUSINESS_DATE);
        simpleOrder(token, simpleOrderRequest("Manual service", "50.00", cashMethodId, null, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Business date is closed."));
        assertThat(count("invoices")).isZero();

        dateWriteGuard.clear();
        dateWriteGuard.failOnCheck(BUSINESS_DATE, 2);
        simpleOrder(token, simpleOrderRequest("Manual service", "50.00", cashMethodId, null, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Simulated completion failure."));
        assertThat(count("invoices")).isZero();
        assertThat(count("invoice_items")).isZero();
        assertThat(count("invoice_payments")).isZero();
        assertThat(countAudit("INVOICE_COMPLETED")).isZero();
    }

    @Test
    void completedSimpleOrdersAppearInInvoiceSearchReportsAndDailyClosing() throws Exception {
        String adminToken = createUserAndLogin("simple-report-admin", Role.ADMIN);
        long employeeId = createEmployee("Simple order stylist", true);
        simpleOrder(adminToken, simpleOrderRequest(
                "Manual hair service", "275.00", paymentMethodId("CASH"), null, employeeId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completedBy").value("simple-report-admin"));

        mockMvc.perform(get("/api/v1/invoices")
                        .param("status", "COMPLETED")
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].finalTotal").value(275.00));

        mockMvc.perform(get("/api/v1/dashboard")
                        .param("filter", "CUSTOM")
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossSales").value(275.00))
                .andExpect(jsonPath("$.netSales").value(275.00))
                .andExpect(jsonPath("$.completedInvoiceCount").value(1))
                .andExpect(jsonPath("$.costOfSoldItemsAndHair").value(0.00))
                .andExpect(jsonPath("$.netProfit").value(275.00))
                .andExpect(jsonPath("$.expectedCashBalance").value(275.00));

        mockMvc.perform(post("/api/v1/daily-closings/{date}/close", BUSINESS_DATE)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new com.omar.beautycenter.closing.dto.CloseDailyRequest(
                                new BigDecimal("275.00"), "Simple order closing"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossSales").value(275.00))
                .andExpect(jsonPath("$.expectedCash").value(275.00))
                .andExpect(jsonPath("$.netProfit").value(275.00));
    }

    @Test
    void receptionistsCanOnlyListAndReadTheirOwnInvoicesWithoutCostFields() throws Exception {
        String firstToken = createUserAndLogin("reception-one", Role.RECEPTIONIST);
        String secondToken = createUserAndLogin("reception-two", Role.RECEPTIONIST);
        long cashMethodId = paymentMethodId("CASH");

        long firstInvoiceId = readJson(simpleOrder(firstToken, simpleOrderRequest(
                        "First receptionist order", "100.00", cashMethodId, null, null))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asLong();
        long secondInvoiceId = readJson(simpleOrder(secondToken, simpleOrderRequest(
                        "Second receptionist order", "200.00", cashMethodId, null, null))
                .andExpect(status().isCreated())
                .andReturn()).path("id").asLong();

        mockMvc.perform(get("/api/v1/invoices")
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString())
                        .param("createdBy", "reception-two")
                        .header(HttpHeaders.AUTHORIZATION, bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(firstInvoiceId));

        mockMvc.perform(get("/api/v1/invoices/{invoiceId}", firstInvoiceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(firstToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitCostSnapshot").doesNotExist());
        mockMvc.perform(get("/api/v1/invoices/{invoiceId}", secondInvoiceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(firstToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @SuppressWarnings("unchecked")
    void quickOrdersUseCatalogSnapshotsIgnoreClientPricesAndAreIdempotent() throws Exception {
        String token = createUserAndLogin("quick-owner", Role.OWNER);
        long firstItemId = createCatalogItem("QUICK_FIRST", "100.00", "30.00");
        long secondItemId = createCatalogItem("QUICK_SECOND", "25.00", "10.00");
        CreateQuickOrderRequest request = quickOrderRequest(
                List.of(
                        new QuickOrderLineRequest(firstItemId, new BigDecimal("2.000"), null, null, null),
                        new QuickOrderLineRequest(secondItemId, new BigDecimal("1.000"), null, null, null)),
                List.of(new QuickOrderPaymentRequest(paymentMethodId("CASH"), new BigDecimal("225.00"), null)),
                "quick-request-001");
        Map<String, Object> payload = objectMapper.convertValue(request, Map.class);
        ((Map<String, Object>) ((List<?>) payload.get("lines")).get(0)).put("unitSellingPrice", "1.00");

        MvcResult firstResult = quickOrder(token, payload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(225.00))
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andExpect(jsonPath("$.lines[0].finalLineTotal").value(200.00))
                .andExpect(jsonPath("$.lines[0].unitCostSnapshot").doesNotExist())
                .andReturn();
        MvcResult retryResult = quickOrder(token, payload)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(225.00))
                .andReturn();

        assertThat(readJson(retryResult).path("invoiceId").asLong())
                .isEqualTo(readJson(firstResult).path("invoiceId").asLong());
        long invoiceId = readJson(firstResult).path("invoiceId").asLong();
        mockMvc.perform(get("/api/v1/invoices/{invoiceId}", invoiceId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Quick customer"))
                .andExpect(jsonPath("$.customerPhone").value("01000000000"))
                .andExpect(jsonPath("$.notes").value("Quick order note"));
        assertThat(count("invoices")).isEqualTo(1);
        assertThat(count("invoice_items")).isEqualTo(2);
        assertThat(count("invoice_payments")).isEqualTo(1);
        assertThat(countAudit("INVOICE_COMPLETED")).isEqualTo(1);

        quickOrder(token, quickOrderRequest(
                        List.of(new QuickOrderLineRequest(firstItemId, new BigDecimal("1.500"), null, null, null)),
                        List.of(new QuickOrderPaymentRequest(paymentMethodId("CASH"), new BigDecimal("150.00"), null)),
                        "quick-fractional-quantity"))
                .andExpect(status().isConflict());
    }

    @Test
    void quickOrdersRejectInactiveItemsRollbackFailuresAndUseHairPricing() throws Exception {
        String token = createUserAndLogin("quick-validation", Role.ADMIN);
        long inactiveItemId = createCatalogItem("QUICK_INACTIVE", "50.00", "10.00");
        jdbcTemplate.update("UPDATE catalog_items SET active = FALSE WHERE id = ?", inactiveItemId);
        quickOrder(token, quickOrderRequest(
                        List.of(new QuickOrderLineRequest(
                                inactiveItemId, new BigDecimal("1.000"), null, null, null)),
                        List.of(new QuickOrderPaymentRequest(paymentMethodId("CASH"), null, null)),
                        "quick-inactive"))
                .andExpect(status().isConflict());
        assertThat(count("invoices")).isZero();

        dateWriteGuard.clear();
        dateWriteGuard.close(BUSINESS_DATE);
        long closedDateItemId = createCatalogItem("QUICK_CLOSED", "50.00", "10.00");
        quickOrder(token, quickOrderRequest(
                        List.of(new QuickOrderLineRequest(
                                closedDateItemId, new BigDecimal("1.000"), null, null, null)),
                        List.of(new QuickOrderPaymentRequest(paymentMethodId("CASH"), null, null)),
                        "quick-closed"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Business date is closed."));
        assertThat(count("invoices")).isZero();

        long failingItemId = createCatalogItem("QUICK_ROLLBACK", "50.00", "10.00");
        dateWriteGuard.clear();
        dateWriteGuard.failOnCheck(BUSINESS_DATE, 2);
        quickOrder(token, quickOrderRequest(
                        List.of(new QuickOrderLineRequest(
                                failingItemId, new BigDecimal("1.000"), null, null, null)),
                        List.of(new QuickOrderPaymentRequest(paymentMethodId("CASH"), null, null)),
                        "quick-rollback"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Simulated completion failure."));
        assertThat(count("invoices")).isZero();
        assertThat(count("invoice_items")).isZero();
        assertThat(count("invoice_payments")).isZero();

        dateWriteGuard.clear();
        long hairItemId = createHairCatalogItem("QUICK_HAIR", "5.00", "2.00");
        long hairConfigurationId = createHairConfiguration(hairItemId, "20.00", "8.00", "15.00");
        jdbcTemplate.update("INSERT INTO hair_product_stocks (hair_item_id, available_grams) VALUES (?, ?)",
                hairItemId, new BigDecimal("10.000"));
        quickOrder(token, quickOrderRequest(
                        List.of(new QuickOrderLineRequest(
                                hairItemId,
                                new BigDecimal("1.000"),
                                null,
                                hairConfigurationId,
                                new BigDecimal("3.000"))),
                        List.of(new QuickOrderPaymentRequest(paymentMethodId("CASH"), new BigDecimal("75.00"), null)),
                        "quick-hair"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(75.00))
                .andExpect(jsonPath("$.lines[0].quantity").value(3.000));
    }

    @Test
    void cashReconciliationCountsCashOrdersOfEveryItemKindButExcludesCardAndWallet() throws Exception {
        String token = createUserAndLogin("cash-reconciliation-owner", Role.OWNER);
        long serviceItemId = createCatalogItem("CASH_SERVICE", "100.00", "25.00");
        long hairItemId = createHairCatalogItem("CASH_HAIR", "5.00", "2.00");
        long hairConfigurationId = createHairConfiguration(hairItemId, "20.00", "8.00", "15.00");
        jdbcTemplate.update("INSERT INTO hair_product_stocks (hair_item_id, available_grams) VALUES (?, ?)",
                hairItemId, new BigDecimal("10.000"));

        quickOrder(token, quickOrderRequest(
                        List.of(new QuickOrderLineRequest(serviceItemId, new BigDecimal("1.000"), null, null, null)),
                        List.of(new QuickOrderPaymentRequest(paymentMethodId("CASH"), new BigDecimal("100.00"), null)),
                        "cash-service"))
                .andExpect(status().isCreated());
        quickOrder(token, quickOrderRequest(
                        List.of(new QuickOrderLineRequest(hairItemId, new BigDecimal("1.000"), null,
                                hairConfigurationId, new BigDecimal("3.000"))),
                        List.of(new QuickOrderPaymentRequest(paymentMethodId("CASH"), new BigDecimal("75.00"), null)),
                        "cash-hair"))
                .andExpect(status().isCreated());
        quickOrder(token, quickOrderRequest(
                        List.of(new QuickOrderLineRequest(serviceItemId, new BigDecimal("1.000"), null, null, null)),
                        List.of(new QuickOrderPaymentRequest(paymentMethodId("VISA"), new BigDecimal("100.00"), null)),
                        "card-service"))
                .andExpect(status().isCreated());
        quickOrder(token, quickOrderRequest(
                        List.of(new QuickOrderLineRequest(serviceItemId, new BigDecimal("1.000"), null, null, null)),
                        List.of(new QuickOrderPaymentRequest(paymentMethodId("INSTAPAY"), new BigDecimal("100.00"), "wallet-ref")),
                        "wallet-service"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/expenses")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExpenseRequest(
                                jdbcTemplate.queryForObject(
                                        "SELECT id FROM expense_categories WHERE name_english = 'Other'", Long.class),
                                new BigDecimal("25.00"), BUSINESS_DATE, paymentMethodId("CASH"),
                                null, "Cash operating expense", null, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("filter", "CUSTOM")
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossSales").value(375.00))
                .andExpect(jsonPath("$.cashPaymentsReceived").value(175.00))
                .andExpect(jsonPath("$.cashExpenses").value(25.00))
                .andExpect(jsonPath("$.expectedCashBalance").value(150.00));

        mockMvc.perform(get("/api/v1/daily-closings/{date}/preview", BUSINESS_DATE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liveFinancials.expectedCashBalance").value(150.00));
    }

    @Test
    void hairStockIsInitializedAdjustedAndChangedOnlyByCompletedHairOrders() throws Exception {
        String ownerToken = createUserAndLogin("hair-stock-owner", Role.OWNER);
        long hairItemId = createHairCatalogItem("STOCK_HAIR", "5.00", "2.00");
        long configurationId = createHairConfiguration(hairItemId, "20.00", "8.00", "15.00");
        jdbcTemplate.update("UPDATE hair_configurations SET minimum_quantity = ? WHERE id = ?",
                new BigDecimal("2.000"), configurationId);

        initializeHairStock(ownerToken, hairItemId, "5.000", "رصيد الجرد الأول")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockConfigured").value(true))
                .andExpect(jsonPath("$.availableGrams").value(5.000));
        mockMvc.perform(get("/api/v1/hair-configurations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("hairItemId", String.valueOf(hairItemId))
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].minimumQuantity").value(2.000))
                .andExpect(jsonPath("$.content[0].stockConfigured").value(true))
                .andExpect(jsonPath("$.content[0].availableStockGrams").value(5.000));

        quickOrder(ownerToken, quickOrderRequest(
                        List.of(new QuickOrderLineRequest(hairItemId, new BigDecimal("1.000"), null,
                                configurationId, new BigDecimal("1.000"))),
                        List.of(new QuickOrderPaymentRequest(paymentMethodId("CASH"), new BigDecimal("35.00"), null)),
                        "hair-below-minimum"))
                .andExpect(status().isConflict());
        assertThat(hairStock(hairItemId)).isEqualByComparingTo("5.000");
        assertThat(count("invoices")).isZero();

        quickOrder(ownerToken, quickOrderRequest(
                        List.of(new QuickOrderLineRequest(hairItemId, new BigDecimal("1.000"), null,
                                configurationId, new BigDecimal("6.000"))),
                        List.of(new QuickOrderPaymentRequest(paymentMethodId("CASH"), new BigDecimal("135.00"), null)),
                        "hair-insufficient"))
                .andExpect(status().isConflict());
        assertThat(hairStock(hairItemId)).isEqualByComparingTo("5.000");
        assertThat(count("invoices")).isZero();

        CreateQuickOrderRequest soldOrder = quickOrderRequest(
                List.of(new QuickOrderLineRequest(hairItemId, new BigDecimal("1.000"), null,
                        configurationId, new BigDecimal("2.000"))),
                List.of(new QuickOrderPaymentRequest(paymentMethodId("CASH"), new BigDecimal("55.00"), null)),
                "hair-stock-sale");
        long invoiceId = readJson(quickOrder(ownerToken, soldOrder)
                .andExpect(status().isCreated())
                .andReturn()).path("invoiceId").asLong();
        quickOrder(ownerToken, soldOrder).andExpect(status().isCreated());
        assertThat(hairStock(hairItemId)).isEqualByComparingTo("3.000");
        assertThat(count("hair_stock_movements WHERE movement_type = 'SALE'")).isEqualTo(1);

        cancel(ownerToken, invoiceId, "إلغاء الأوردر")
                .andExpect(status().isOk());
        assertThat(hairStock(hairItemId)).isEqualByComparingTo("5.000");
        assertThat(count("hair_stock_movements WHERE movement_type = 'CANCELLATION_RESTORE'")).isEqualTo(1);
        assertThat(countAudit("HAIR_STOCK_INITIALIZED")).isEqualTo(1);
        assertThat(countAudit("HAIR_STOCK_SOLD")).isEqualTo(1);
        assertThat(countAudit("HAIR_STOCK_CANCELLATION_RESTORED")).isEqualTo(1);
    }

    @Test
    void returnedHairGramsRestoreOnlyTheExplicitRefundQuantityAndManualAdjustmentsAreAudited() throws Exception {
        String ownerToken = createUserAndLogin("hair-refund-owner", Role.OWNER);
        long hairItemId = createHairCatalogItem("REFUND_HAIR", "5.00", "2.00");
        long configurationId = createHairConfiguration(hairItemId, "20.00", "8.00", "15.00");
        initializeHairStock(ownerToken, hairItemId, "5.000", "بداية المخزون")
                .andExpect(status().isCreated());
        adjustHairStock(ownerToken, hairItemId, "1.000", "توريد جديد")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableGrams").value(6.000));
        adjustHairStock(ownerToken, hairItemId, "-10.000", "جرد")
                .andExpect(status().isConflict());
        assertThat(hairStock(hairItemId)).isEqualByComparingTo("6.000");

        long invoiceId = readJson(quickOrder(ownerToken, quickOrderRequest(
                        List.of(new QuickOrderLineRequest(hairItemId, new BigDecimal("1.000"), null,
                                configurationId, new BigDecimal("3.000"))),
                        List.of(new QuickOrderPaymentRequest(paymentMethodId("CASH"), new BigDecimal("75.00"), null)),
                        "hair-refund-sale"))
                .andExpect(status().isCreated())
                .andReturn()).path("invoiceId").asLong();
        long invoiceItemId = jdbcTemplate.queryForObject(
                "SELECT id FROM invoice_items WHERE invoice_id = ?", Long.class, invoiceId);
        assertThat(hairStock(hairItemId)).isEqualByComparingTo("3.000");

        CreateInvoiceRefundRequest refund = new CreateInvoiceRefundRequest(
                BUSINESS_DATE,
                new BigDecimal("20.00"),
                "إرجاع جزء من الشعر",
                List.of(payment("CASH", "20.00", null)),
                List.of(new HairRefundReturnRequest(invoiceItemId, new BigDecimal("1.000"))));
        mockMvc.perform(post("/api/v1/invoices/{invoiceId}/refunds", invoiceId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refund)))
                .andExpect(status().isCreated());
        assertThat(hairStock(hairItemId)).isEqualByComparingTo("4.000");
        assertThat(count("hair_stock_movements WHERE movement_type = 'REFUND_RESTORE'")).isEqualTo(1);
        assertThat(countAudit("HAIR_STOCK_ADJUSTED")).isEqualTo(1);
        assertThat(countAudit("HAIR_STOCK_REFUND_RESTORED")).isEqualTo(1);
    }

    @Test
    void onlyOwnerCanEnterOrAdjustHairStock() throws Exception {
        String ownerToken = createUserAndLogin("stock-owner", Role.OWNER);
        String adminToken = createUserAndLogin("stock-admin", Role.ADMIN);
        long hairItemId = createHairCatalogItem("OWNER_STOCK", "5.00", "2.00");

        initializeHairStock(adminToken, hairItemId, "5.000", "غير مسموح")
                .andExpect(status().isForbidden());
        initializeHairStock(ownerToken, hairItemId, "5.000", "رصيد صحيح")
                .andExpect(status().isCreated());
        adjustHairStock(adminToken, hairItemId, "1.000", "غير مسموح")
                .andExpect(status().isForbidden());
        assertThat(hairStock(hairItemId)).isEqualByComparingTo("5.000");
    }

    private long createDraft(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/invoices")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.invoiceNumber").isNotEmpty())
                .andReturn();
        return readJson(result).path("id").asLong();
    }

    private long createDraftWithItem(String token, long itemId, String quantity) throws Exception {
        long invoiceId = createDraft(token);
        addItem(token, invoiceId, itemRequest(itemId, quantity, null, null))
                .andExpect(status().isCreated());
        return invoiceId;
    }

    private ResultActions addItem(
            String token,
            long invoiceId,
            UpsertInvoiceItemRequest request) throws Exception {
        return mockMvc.perform(post("/api/v1/invoices/{invoiceId}/items", invoiceId)
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions simpleOrder(String token, CreateSimpleOrderRequest request) throws Exception {
        return mockMvc.perform(post("/api/v1/invoices/simple-orders")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions quickOrder(String token, Object request) throws Exception {
        return mockMvc.perform(post("/api/v1/invoices/quick-orders")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions initializeHairStock(String token, long hairItemId, String quantity, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/hair-stock/{hairItemId}/initialize", hairItemId)
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("quantityGrams", quantity, "reason", reason))));
    }

    private ResultActions adjustHairStock(String token, long hairItemId, String adjustment, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/hair-stock/{hairItemId}/adjustments", hairItemId)
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("adjustmentGrams", adjustment, "reason", reason))));
    }

    private static CreateQuickOrderRequest quickOrderRequest(
            List<QuickOrderLineRequest> lines,
            List<QuickOrderPaymentRequest> payments,
            String clientRequestId) {
        return new CreateQuickOrderRequest(
                BUSINESS_DATE,
                lines,
                payments,
                clientRequestId,
                "Quick customer",
                "01000000000",
                "Quick order note");
    }

    private static CreateSimpleOrderRequest simpleOrderRequest(
            String description,
            String amount,
            Long paymentMethodId,
            String paymentReference,
            Long employeeId) {
        return new CreateSimpleOrderRequest(
                "Simple customer",
                "01000000000",
                BUSINESS_DATE,
                description,
                new BigDecimal(amount),
                paymentMethodId,
                paymentReference,
                employeeId,
                "Simple order note");
    }

    private ResultActions setPayments(
            String token,
            long invoiceId,
            InvoicePaymentRequest... payments) throws Exception {
        return mockMvc.perform(put("/api/v1/invoices/{invoiceId}/payments", invoiceId)
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new SetInvoicePaymentsRequest(List.of(payments)))));
    }

    private ResultActions complete(String token, long invoiceId) throws Exception {
        return mockMvc.perform(post("/api/v1/invoices/{invoiceId}/complete", invoiceId)
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private ResultActions cancel(
            String token,
            long invoiceId,
            String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/invoices/{invoiceId}/cancel", invoiceId)
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CancelInvoiceRequest(reason))));
    }

    private ResultActions refund(
            String token,
            long invoiceId,
            String amount,
            String reason) throws Exception {
        return refund(token, invoiceId, BUSINESS_DATE, amount, reason);
    }

    private ResultActions refund(
            String token,
            long invoiceId,
            LocalDate businessDate,
            String amount,
            String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/invoices/{invoiceId}/refunds", invoiceId)
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateInvoiceRefundRequest(
                        businessDate,
                        new BigDecimal(amount),
                        reason,
                        List.of(payment("CASH", amount, null))))));
    }

    private InvoicePaymentRequest payment(String code, String amount, String reference) {
        return new InvoicePaymentRequest(paymentMethodId(code), new BigDecimal(amount), reference);
    }

    private long paymentMethodId(String code) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM payment_methods WHERE code = ?",
                Long.class,
                code);
    }

    private BigDecimal hairStock(long hairItemId) {
        return jdbcTemplate.queryForObject(
                "SELECT available_grams FROM hair_product_stocks WHERE hair_item_id = ?",
                BigDecimal.class,
                hairItemId);
    }

    private long createEmployee(String name, boolean active) {
        jdbcTemplate.update(
                "INSERT INTO employees (name, position, default_daily_wage, active) VALUES (?, ?, ?, ?)",
                name,
                "Stylist",
                new BigDecimal("0.00"),
                active);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM employees WHERE name = ?", Long.class, name);
    }

    private long createCatalogItem(String code, String sellingPrice, String cost) {
        String categoryEnglish = "Sales " + code;
        jdbcTemplate.update(
                "INSERT INTO catalog_categories (name_arabic, name_english, active) VALUES (?, ?, TRUE)",
                "Arabic " + code,
                categoryEnglish);
        Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_categories WHERE name_english = ?",
                Long.class,
                categoryEnglish);
        jdbcTemplate.update(
                """
                INSERT INTO catalog_items (
                    code, name_arabic, name_english, item_type, category_id,
                    current_selling_price, current_cost, active)
                VALUES (?, ?, ?, 'SERVICE', ?, ?, ?, TRUE)
                """,
                code,
                "Arabic " + code,
                "English " + code,
                categoryId,
                new BigDecimal(sellingPrice),
                new BigDecimal(cost));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_items WHERE code = ?",
                Long.class,
                code);
    }

    private long createHairCatalogItem(String code, String sellingPrice, String cost) {
        String categoryEnglish = "Hair " + code;
        jdbcTemplate.update(
                "INSERT INTO catalog_categories (name_arabic, name_english, active) VALUES (?, ?, TRUE)",
                "Arabic " + code,
                categoryEnglish);
        Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_categories WHERE name_english = ?",
                Long.class,
                categoryEnglish);
        jdbcTemplate.update(
                """
                INSERT INTO catalog_items (
                    code, name_arabic, name_english, item_type, category_id,
                    current_selling_price, current_cost, active)
                VALUES (?, ?, ?, 'HAIR_PRODUCT', ?, ?, ?, TRUE)
                """,
                code,
                "Arabic " + code,
                "English " + code,
                categoryId,
                new BigDecimal(sellingPrice),
                new BigDecimal(cost));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM catalog_items WHERE code = ?",
                Long.class,
                code);
    }

    private long createHairConfiguration(
            long hairItemId,
            String sellingPrice,
            String cost,
            String installationFee) {
        jdbcTemplate.update(
                """
                INSERT INTO hair_configurations (
                    hair_item_id, brand, hair_length, unit_type,
                    unit_selling_price, unit_cost, installation_fee,
                    active, effective_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, TRUE, ?)
                """,
                hairItemId,
                "Quick brand",
                "20",
                "gram",
                new BigDecimal(sellingPrice),
                new BigDecimal(cost),
                new BigDecimal(installationFee),
                BUSINESS_DATE);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM hair_configurations WHERE hair_item_id = ?",
                Long.class,
                hairItemId);
    }

    private long createDiscount(Long itemId, DiscountType type, String value) {
        jdbcTemplate.update(
                """
                INSERT INTO discounts (
                    name, discount_type, discount_value, start_date, end_date,
                    active, catalog_item_id)
                VALUES (?, ?, ?, ?, ?, TRUE, ?)
                """,
                "Invoice discount",
                type.name(),
                new BigDecimal(value),
                BUSINESS_DATE.minusDays(1),
                BUSINESS_DATE.plusDays(1),
                itemId);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM discounts WHERE name = 'Invoice discount'",
                Long.class);
    }

    private String createUserAndLogin(String username, Role role) throws Exception {
        return createUserAndLogin(username, role, null);
    }

    private String createUserAndLogin(
            String username,
            Role role,
            Set<SystemPermission> permissions) throws Exception {
        User user = new User(
                username,
                "Test " + role.name(),
                passwordEncoder.encode(PASSWORD),
                true,
                Set.of(role));
        if (permissions != null) {
            user.setPermissions(permissions);
        }
        userRepository.saveAndFlush(user);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(username, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("accessToken").asText();
    }

    private static CreateDraftInvoiceRequest draftRequest() {
        return new CreateDraftInvoiceRequest(
                BUSINESS_DATE,
                "Customer",
                "01000000000",
                "Phase 5 test invoice");
    }

    private static UpsertInvoiceItemRequest itemRequest(
            long itemId,
            String quantity,
            Long discountId,
            BigDecimal overrideAmount) {
        return new UpsertInvoiceItemRequest(
                itemId,
                null,
                new BigDecimal(quantity),
                discountId,
                null,
                overrideAmount,
                overrideAmount == null ? null : "Authorized test override");
    }

    private String statusOf(long invoiceId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM invoices WHERE id = ?",
                String.class,
                invoiceId);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private int countAudit(String action) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = ?", Integer.class, action);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @TestConfiguration
    static class ClosedDateTestConfiguration {

        @Bean
        @Primary
        TestBusinessDateWriteGuard testBusinessDateWriteGuard() {
            return new TestBusinessDateWriteGuard();
        }
    }

    static class TestBusinessDateWriteGuard implements BusinessDateWriteGuard {

        private final Set<LocalDate> closedDates = ConcurrentHashMap.newKeySet();
        private final Map<LocalDate, Integer> checks = new ConcurrentHashMap<>();
        private final Map<LocalDate, Integer> failureChecks = new ConcurrentHashMap<>();

        @Override
        public void assertWritable(LocalDate businessDate) {
            if (closedDates.contains(businessDate)) {
                throw new BusinessRuleException("Business date is closed.");
            }
            int check = checks.merge(businessDate, 1, Integer::sum);
            if (Integer.valueOf(check).equals(failureChecks.get(businessDate))) {
                throw new BusinessRuleException("Simulated completion failure.");
            }
        }

        void close(LocalDate date) {
            closedDates.add(date);
        }

        void failOnCheck(LocalDate date, int check) {
            failureChecks.put(date, check);
        }

        void clear() {
            closedDates.clear();
            checks.clear();
            failureChecks.clear();
        }
    }
}
