// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.expense;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omar.beautycenter.auth.dto.LoginRequest;
import com.omar.beautycenter.closing.BusinessDateWriteGuard;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.expense.dto.CancelExpenseRequest;
import com.omar.beautycenter.expense.dto.CreateExpenseCategoryRequest;
import com.omar.beautycenter.expense.dto.CreateExpenseRequest;
import com.omar.beautycenter.expense.dto.UpdateExpenseCategoryRequest;
import com.omar.beautycenter.payment.PaymentMethod;
import com.omar.beautycenter.payment.PaymentMethodRepository;
import com.omar.beautycenter.payment.PaymentMethodType;
import com.omar.beautycenter.payment.dto.CreatePaymentMethodRequest;
import com.omar.beautycenter.payment.dto.PaymentMethodActiveRequest;
import com.omar.beautycenter.payment.dto.UpdatePaymentMethodRequest;
import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.SystemPermission;
import com.omar.beautycenter.user.User;
import com.omar.beautycenter.user.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseIntegrationTest {

    // TEST ONLY - synthetic password.
    private static final String PASSWORD = "test-only-strong-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private ExpenseCategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @MockitoBean
    private BusinessDateWriteGuard businessDateWriteGuard;

    @BeforeEach
    void cleanDatabase() {
        reset(businessDateWriteGuard);
        jdbcTemplate.update("DELETE FROM expenses");
        jdbcTemplate.update("DELETE FROM expense_categories WHERE created_by <> 'SYSTEM'");
        jdbcTemplate.update("DELETE FROM payment_methods WHERE created_by <> 'SYSTEM'");
        userRepository.deleteAll();
    }

    @Test
    void flywaySeedsConfigurablePaymentMethodsAndBilingualCategories() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);

        mockMvc.perform(get("/api/v1/payment-methods")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("active", "true")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content[0].code").value("CASH"))
                .andExpect(jsonPath("$.content[0].type").value("CASH"));

        mockMvc.perform(get("/api/v1/expense-categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("active", "true")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(9));
    }

    @Test
    void managerCanManageMasterDataButReceptionistCanOnlyReadIt() throws Exception {
        String managerToken = createUserAndLogin("admin", Role.ADMIN);
        long categoryId = createCategory(managerToken, "اختبار", "Test category", true);
        long methodId = createPaymentMethod(
                managerToken, "bank_transfer", PaymentMethodType.OTHER, true, true, 60);

        mockMvc.perform(put("/api/v1/expense-categories/{categoryId}", categoryId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateExpenseCategoryRequest(
                                "اختبار معدل", "Updated test category", "Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameEnglish").value("Updated test category"));

        mockMvc.perform(patch("/api/v1/payment-methods/{methodId}/active", methodId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PaymentMethodActiveRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(put("/api/v1/payment-methods/{methodId}", methodId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePaymentMethodRequest(
                                "تحويل بنكي",
                                "Bank transfer updated",
                                PaymentMethodType.OTHER,
                                true,
                                65))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.displayNameEnglish").value("Bank transfer updated"));

        String receptionistToken = createUserAndLogin(
                "reception", Role.RECEPTIONIST, Set.of(SystemPermission.EXPENSES));
        mockMvc.perform(get("/api/v1/payment-methods")
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/expense-categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/expense-categories")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExpenseCategoryRequest(
                                "ممنوع", "Blocked", true, null))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/payment-methods")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentMethodRequest(
                                "BLOCKED", PaymentMethodType.OTHER, true, false, 90))))
                .andExpect(status().isForbidden());
    }

    @Test
    void expenseValidationRejectsInvalidAmountsAndUnsafeReceiptPaths() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long categoryId = seedCategoryId("Laundry");
        long cashId = seedPaymentMethodId("CASH");
        CreateExpenseRequest invalid = new CreateExpenseRequest(
                categoryId,
                BigDecimal.ZERO,
                currentDate(),
                cashId,
                null,
                " ",
                null,
                null);

        mockMvc.perform(post("/api/v1/expenses")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.length()").value(2));

        CreateExpenseRequest unsafePath = expenseRequest(
                categoryId, cashId, "10.00", currentDate(), null, "../outside.jpg");
        mockMvc.perform(post("/api/v1/expenses")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unsafePath)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void inactiveCategoryAndPaymentMethodCannotBeUsed() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long inactiveCategory = createCategory(token, "غير نشط", "Inactive category", false);
        long activeCategory = createCategory(token, "نشط", "Active category", true);
        long activeMethod = createPaymentMethod(
                token, "ACTIVE_TEST", PaymentMethodType.OTHER, true, false, 70);
        long inactiveMethod = createPaymentMethod(
                token, "INACTIVE_TEST", PaymentMethodType.OTHER, false, false, 71);

        performCreateExpense(token, expenseRequest(
                        inactiveCategory, activeMethod, "10.00", currentDate(), null, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Inactive expense categories cannot be used for new expenses."));

        performCreateExpense(token, expenseRequest(
                        activeCategory, inactiveMethod, "10.00", currentDate(), null, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Inactive payment methods cannot be used for new records."));
    }

    @Test
    void configuredPaymentMethodsAllowMissingReferencesAndPreserveOptionalMetadata() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long categoryId = seedCategoryId("Maintenance");
        long instaPayId = seedPaymentMethodId("INSTAPAY");

        performCreateExpense(token, expenseRequest(
                        categoryId, instaPayId, "80.00", currentDate(), null, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethodCode").value("INSTAPAY"))
                .andExpect(jsonPath("$.paymentReference").isEmpty())
                .andExpect(jsonPath("$.receiptPath").isEmpty());

        performCreateExpense(token, expenseRequest(
                        categoryId, instaPayId, "80.00", currentDate(), "IP-123", "receipts/instapay.jpg"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethodCode").value("INSTAPAY"))
                .andExpect(jsonPath("$.paymentReference").value("IP-123"))
                .andExpect(jsonPath("$.receiptPath").value("receipts/instapay.jpg"));
    }

    @Test
    void cancellationPreservesHistoryFiltersCorrectlyAndExcludesTotals() throws Exception {
        String token = createUserAndLogin("admin", Role.ADMIN);
        long categoryId = seedCategoryId("Purchases");
        long cashId = seedPaymentMethodId("CASH");
        long cancelledId = createExpense(token, expenseRequest(
                categoryId, cashId, "40.00", currentDate(), null, null), "Cancel target");
        createExpense(token, expenseRequest(
                categoryId, cashId, "60.00", currentDate(), null, null), "Keep target");

        mockMvc.perform(post("/api/v1/expenses/{expenseId}/cancel", cancelledId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CancelExpenseRequest("Duplicate expense"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledBy").value("admin"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/expenses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("categoryId", Long.toString(categoryId))
                        .param("paymentMethodId", Long.toString(cashId))
                        .param("status", "CANCELLED")
                        .param("fromDate", currentDate().toString())
                        .param("toDate", currentDate().toString())
                        .param("query", "cancel target"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(cancelledId));

        mockMvc.perform(get("/api/v1/expenses/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("fromDate", currentDate().toString())
                        .param("toDate", currentDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeExpenseCount").value(1))
                .andExpect(jsonPath("$.cancelledExpenseCount").value(1))
                .andExpect(jsonPath("$.totalAmount").value(60.00));

        mockMvc.perform(post("/api/v1/expenses/{expenseId}/cancel", cancelledId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CancelExpenseRequest("Again"))))
                .andExpect(status().isConflict());
    }

    @Test
    void summariesGroupActiveExpensesAndUsePaymentTypeForCashTotals() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long laundry = seedCategoryId("Laundry");
        long maintenance = seedCategoryId("Maintenance");
        long cash = seedPaymentMethodId("CASH");
        long card = createPaymentMethod(
                token, "CUSTOM_CARD", PaymentMethodType.CARD, true, false, 25);
        long instaPay = seedPaymentMethodId("INSTAPAY");

        createExpense(token, expenseRequest(
                laundry, cash, "100.00", currentDate(), null, null), "Cash laundry");
        createExpense(token, expenseRequest(
                maintenance, card, "50.00", currentDate(), null, null), "Card maintenance");
        createExpense(token, expenseRequest(
                maintenance, instaPay, "25.00", currentDate(), "REF-25", null), "Wallet maintenance");

        mockMvc.perform(put("/api/v1/payment-methods/{paymentMethodId}", card)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePaymentMethodRequest(
                                "بطاقة معدلة",
                                "Changed to cash later",
                                PaymentMethodType.CASH,
                                false,
                                25))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CASH"));

        mockMvc.perform(get("/api/v1/expenses/summary")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("fromDate", currentDate().toString())
                        .param("toDate", currentDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(175.00))
                .andExpect(jsonPath("$.cashAmount").value(100.00))
                .andExpect(jsonPath("$.nonCashAmount").value(75.00));

        mockMvc.perform(get("/api/v1/expenses/summary/by-category")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("fromDate", currentDate().toString())
                        .param("toDate", currentDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nameEnglish").value("Laundry"))
                .andExpect(jsonPath("$[0].totalAmount").value(100.00))
                .andExpect(jsonPath("$[1].nameEnglish").value("Maintenance"))
                .andExpect(jsonPath("$[1].totalAmount").value(75.00));

        mockMvc.perform(get("/api/v1/expenses/summary/by-payment-method")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("fromDate", currentDate().toString())
                        .param("toDate", currentDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CASH"))
                .andExpect(jsonPath("$[0].totalAmount").value(100.00))
                .andExpect(jsonPath("$[1].code").value("CUSTOM_CARD"))
                .andExpect(jsonPath("$[1].type").value("CARD"))
                .andExpect(jsonPath("$[2].code").value("INSTAPAY"));
    }

    @Test
    void staffMemberWithExpensesPermissionCanManageExpensesOnAnyBusinessDate() throws Exception {
        String receptionistToken = createUserAndLogin(
                "reception", Role.RECEPTIONIST, Set.of(SystemPermission.EXPENSES));
        long categoryId = seedCategoryId("Transportation");
        long cashId = seedPaymentMethodId("CASH");
        long currentId = createExpense(receptionistToken, expenseRequest(
                categoryId, cashId, "20.00", currentDate(), null, null), "Current expense");
        long oldId = createExpense(receptionistToken, expenseRequest(
                categoryId, cashId, "30.00", currentDate().minusDays(1), null, null), "Old expense");

        performCancel(receptionistToken, currentId, "Current correction")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        performCancel(receptionistToken, oldId, "Old correction")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelledBy").value("reception"));
    }

    @Test
    void closingGuardRejectsExpenseCreationAndCancellation() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long categoryId = seedCategoryId("Electricity");
        long cashId = seedPaymentMethodId("CASH");
        CreateExpenseRequest request = expenseRequest(
                categoryId, cashId, "90.00", currentDate(), null, null);

        doThrow(new BusinessRuleException("Business date is closed."))
                .when(businessDateWriteGuard)
                .assertWritable(currentDate());
        performCreateExpense(token, request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Business date is closed."));

        reset(businessDateWriteGuard);
        long expenseId = createExpense(token, request, "Guarded expense");
        doThrow(new BusinessRuleException("Business date is closed."))
                .when(businessDateWriteGuard)
                .assertWritable(currentDate());
        performCancel(token, expenseId, "Should be blocked")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Business date is closed."));
        mockMvc.perform(get("/api/v1/expenses/{expenseId}", expenseId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private long createCategory(
            String token,
            String nameArabic,
            String nameEnglish,
            boolean active) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/expense-categories")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateExpenseCategoryRequest(
                                nameArabic, nameEnglish, active, null))))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private long createPaymentMethod(
            String token,
            String code,
            PaymentMethodType type,
            boolean active,
            boolean requiresReference,
            int sortOrder) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/payment-methods")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentMethodRequest(
                                code, type, active, requiresReference, sortOrder))))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private static CreatePaymentMethodRequest paymentMethodRequest(
            String code,
            PaymentMethodType type,
            boolean active,
            boolean requiresReference,
            int sortOrder) {
        return new CreatePaymentMethodRequest(
                code,
                "طريقة " + code,
                "Method " + code,
                type,
                active,
                requiresReference,
                sortOrder);
    }

    private long createExpense(
            String token,
            CreateExpenseRequest baseRequest,
            String description) throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                baseRequest.categoryId(),
                baseRequest.amount(),
                baseRequest.businessDate(),
                baseRequest.paymentMethodId(),
                baseRequest.paymentReference(),
                description,
                baseRequest.notes(),
                baseRequest.receiptPath());
        MvcResult result = performCreateExpense(token, request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        return readId(result);
    }

    private org.springframework.test.web.servlet.ResultActions performCreateExpense(
            String token,
            CreateExpenseRequest request) throws Exception {
        return mockMvc.perform(post("/api/v1/expenses")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private org.springframework.test.web.servlet.ResultActions performCancel(
            String token,
            long expenseId,
            String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/expenses/{expenseId}/cancel", expenseId)
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CancelExpenseRequest(reason))));
    }

    private static CreateExpenseRequest expenseRequest(
            long categoryId,
            long paymentMethodId,
            String amount,
            LocalDate businessDate,
            String paymentReference,
            String receiptPath) {
        return new CreateExpenseRequest(
                categoryId,
                new BigDecimal(amount),
                businessDate,
                paymentMethodId,
                paymentReference,
                "Test expense",
                null,
                receiptPath);
    }

    private long seedPaymentMethodId(String code) {
        PaymentMethod method = paymentMethodRepository.findByCode(code).orElseThrow();
        return method.getId();
    }

    private long seedCategoryId(String nameEnglish) {
        return categoryRepository.findByNameEnglishIgnoreCase(nameEnglish).orElseThrow().getId();
    }

    private LocalDate currentDate() {
        return LocalDate.now(clock);
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
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accessToken")
                .asText();
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("id").asLong();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
