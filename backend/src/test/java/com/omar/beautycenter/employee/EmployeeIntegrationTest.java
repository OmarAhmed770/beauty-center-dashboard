// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.employee;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.omar.beautycenter.employee.dto.CancelEmployeeTransactionRequest;
import com.omar.beautycenter.employee.dto.CreateEmployeeRequest;
import com.omar.beautycenter.employee.dto.CreateEmployeeTransactionRequest;
import com.omar.beautycenter.employee.dto.EmployeeActiveRequest;
import com.omar.beautycenter.employee.dto.UpdateEmployeeRequest;
import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.SystemPermission;
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
class EmployeeIntegrationTest {

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
    private EmployeeRepository employeeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM employee_attendances");
        jdbcTemplate.update("DELETE FROM employee_transactions");
        employeeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void employeeValidationRejectsBlankFieldsAndNegativeWage() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                " ", null, "", new BigDecimal("-0.01"), null);

        mockMvc.perform(post("/api/v1/employees")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.length()").value(3));
    }

    @Test
    void employeeCrudSearchAndActivationArePaginated() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long employeeId = createEmployee(token, "Nour Ali");
        createEmployee(token, "Mona Hassan");

        UpdateEmployeeRequest update = new UpdateEmployeeRequest(
                "Nour Ahmed", "01000000000", "Senior Stylist", new BigDecimal("325.50"), "Updated");
        mockMvc.perform(put("/api/v1/employees/{employeeId}", employeeId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nour Ahmed"))
                .andExpect(jsonPath("$.defaultDailyWage").value(325.50));

        setEmployeeActive(token, employeeId, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("query", "nour")
                        .param("active", "false")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(employeeId))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(1));

        setEmployeeActive(token, employeeId, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void transactionCreationAndCombinedFiltersReturnSafeDtos() throws Exception {
        String token = createUserAndLogin("admin", Role.ADMIN);
        long employeeId = createEmployee(token, "Salma Adel");

        JsonNode created = createTransaction(
                token,
                employeeId,
                EmployeeTransactionType.ADVANCE,
                "125.50",
                "CASH");

        mockMvc.perform(get("/api/v1/employee-transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("employeeId", Long.toString(employeeId))
                        .param("type", "ADVANCE")
                        .param("status", "ACTIVE")
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(created.path("id").asLong()))
                .andExpect(jsonPath("$.content[0].amount").value(125.50))
                .andExpect(jsonPath("$.content[0].paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.content[0].createdBy").value("admin"))
                .andExpect(jsonPath("$.content[0].employee.passwordHash").doesNotExist());
    }

    @Test
    void statementAndSearchPreserveOptionalFiltersDatesAndPagination() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long employeeId = createEmployee(token, "Demo Employee");
        long otherId = createEmployee(token, "Other Employee");
        long wageId = createTransaction(token, employeeId, EmployeeTransactionType.DAILY_WAGE, "100.00", null)
                .path("id").asLong();
        long bonusId = createTransaction(token, employeeId, EmployeeTransactionType.BONUS, "25.00", null, BUSINESS_DATE.plusDays(1))
                .path("id").asLong();
        createTransaction(token, otherId, EmployeeTransactionType.PAYMENT, "10.00", "CASH");

        mockMvc.perform(get("/api/v1/employee-transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].id").value(bonusId));
        for (int page = 0; page < 2; page++) {
            mockMvc.perform(get("/api/v1/employees/{employeeId}/statement", employeeId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .param("fromDate", BUSINESS_DATE.toString())
                            .param("toDate", BUSINESS_DATE.plusDays(1).toString())
                            .param("page", Integer.toString(page)).param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totals.netEntitlement").value(125.00))
                    .andExpect(jsonPath("$.transactions.totalElements").value(2))
                    .andExpect(jsonPath("$.transactions.content[0].id").value(page == 0 ? wageId : bonusId));
        }
        mockMvc.perform(get("/api/v1/employee-transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("employeeId", Long.toString(employeeId)).param("type", "BONUS")
                        .param("status", "ACTIVE").param("fromDate", BUSINESS_DATE.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(bonusId));
        mockMvc.perform(get("/api/v1/employee-transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("toDate", BUSINESS_DATE.minusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void transactionValidationRejectsInvalidAmountAndMissingBusinessDate() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long employeeId = createEmployee(token, "Dina Samir");

        CreateEmployeeTransactionRequest invalid = new CreateEmployeeTransactionRequest(
                employeeId,
                EmployeeTransactionType.BONUS,
                BigDecimal.ZERO,
                null,
                null,
                null);

        mockMvc.perform(post("/api/v1/employee-transactions")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors.length()").value(2));

        mockMvc.perform(get("/api/v1/employees/{employeeId}/totals", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("toDate", BUSINESS_DATE.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETERS"));
    }

    @Test
    void inactiveEmployeeCannotReceiveANewTransaction() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long employeeId = createEmployee(token, "Rana Khaled");
        setEmployeeActive(token, employeeId, false).andExpect(status().isOk());

        CreateEmployeeTransactionRequest request = transactionRequest(
                employeeId, EmployeeTransactionType.DAILY_WAGE, "300.00", null);
        mockMvc.perform(post("/api/v1/employee-transactions")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value(
                        "Transactions can only be created for an active employee."));
    }

    @Test
    void cancellationPreservesHistoryAndExcludesTheTransactionFromTotals() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long employeeId = createEmployee(token, "Heba Omar");
        long transactionId = createTransaction(
                        token, employeeId, EmployeeTransactionType.BONUS, "75.00", null)
                .path("id")
                .asLong();

        CancelEmployeeTransactionRequest cancellation =
                new CancelEmployeeTransactionRequest("Entered for the wrong employee");
        mockMvc.perform(post("/api/v1/employee-transactions/{transactionId}/cancel", transactionId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancellation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty())
                .andExpect(jsonPath("$.cancelledBy").value("owner"))
                .andExpect(jsonPath("$.cancellationReason").value("Entered for the wrong employee"));

        mockMvc.perform(get("/api/v1/employees/{employeeId}/totals", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bonuses").value(0.00))
                .andExpect(jsonPath("$.netEntitlement").value(0.00));

        mockMvc.perform(get("/api/v1/employees/{employeeId}/statement", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions.content.length()").value(1))
                .andExpect(jsonPath("$.transactions.content[0].status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/employee-transactions/{transactionId}/cancel", transactionId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancellation)))
                .andExpect(status().isConflict());
    }

    @Test
    void balanceFormulaSubtractsDeductionsAdvancesAndPaymentsExactlyOnce() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long employeeId = createEmployee(token, "Aya Mostafa");

        createTransaction(token, employeeId, EmployeeTransactionType.DAILY_WAGE, "100.00", null);
        createTransaction(token, employeeId, EmployeeTransactionType.BONUS, "20.00", null);
        createTransaction(token, employeeId, EmployeeTransactionType.COMMISSION, "30.00", null);
        createTransaction(token, employeeId, EmployeeTransactionType.DEDUCTION, "10.00", null);
        createTransaction(token, employeeId, EmployeeTransactionType.ADVANCE, "15.00", "CASH");
        createTransaction(token, employeeId, EmployeeTransactionType.PAYMENT, "25.00", "CASH");

        mockMvc.perform(get("/api/v1/employees/{employeeId}/totals", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyWages").value(100.00))
                .andExpect(jsonPath("$.bonuses").value(20.00))
                .andExpect(jsonPath("$.commissions").value(30.00))
                .andExpect(jsonPath("$.deductions").value(10.00))
                .andExpect(jsonPath("$.advances").value(15.00))
                .andExpect(jsonPath("$.payments").value(25.00))
                .andExpect(jsonPath("$.netEntitlement").value(100.00));
    }

    @Test
    void paidEntitlementsAdvanceAndPaymentsKeepEmployeeBalanceProfitAndCashSeparate() throws Exception {
        LocalDate financialDate = BUSINESS_DATE.plusYears(1);
        String token = createUserAndLogin("employee-financial-owner", Role.OWNER);
        long employeeId = createEmployee(token, "Mariam Financial");

        createTransaction(token, employeeId, EmployeeTransactionType.DAILY_WAGE, "100.00", null, financialDate);
        createTransaction(token, employeeId, EmployeeTransactionType.BONUS, "20.00", "CASH", financialDate);
        createTransaction(token, employeeId, EmployeeTransactionType.COMMISSION, "30.00", "VISA", financialDate);
        createTransaction(token, employeeId, EmployeeTransactionType.ADVANCE, "15.00", "CASH", financialDate);
        createTransaction(token, employeeId, EmployeeTransactionType.PAYMENT, "25.00", "CASH", financialDate);
        createTransaction(token, employeeId, EmployeeTransactionType.ADVANCE, "5.00", "CASH", financialDate);
        createTransaction(token, employeeId, EmployeeTransactionType.PAYMENT, "10.00", "CASH", financialDate);

        mockMvc.perform(get("/api/v1/employees/{employeeId}/totals", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("fromDate", financialDate.toString())
                        .param("toDate", financialDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyWages").value(100.00))
                .andExpect(jsonPath("$.bonuses").value(20.00))
                .andExpect(jsonPath("$.commissions").value(30.00))
                .andExpect(jsonPath("$.advances").value(20.00))
                .andExpect(jsonPath("$.payments").value(35.00))
                .andExpect(jsonPath("$.netEntitlement").value(45.00));

        mockMvc.perform(get("/api/v1/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("filter", "CUSTOM")
                        .param("fromDate", financialDate.toString())
                        .param("toDate", financialDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyWages").value(100.00))
                .andExpect(jsonPath("$.bonuses").value(20.00))
                .andExpect(jsonPath("$.commissions").value(30.00))
                .andExpect(jsonPath("$.employeeCost").value(150.00))
                .andExpect(jsonPath("$.netProfit").value(-150.00))
                .andExpect(jsonPath("$.cashEmployeePayments").value(75.00))
                .andExpect(jsonPath("$.expectedCashBalance").value(-75.00));

        mockMvc.perform(get("/api/v1/reports/financial")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("fromDate", financialDate.toString())
                        .param("toDate", financialDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCost").value(150.00))
                .andExpect(jsonPath("$.netProfit").value(-150.00));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM expenses WHERE business_date = ?", Integer.class, financialDate))
                .isZero();
    }

    @Test
    void staffMemberWithEmployeesPermissionCanManageEmployeeTransactions() throws Exception {
        String ownerToken = createUserAndLogin("owner", Role.OWNER);
        long employeeId = createEmployee(ownerToken, "Laila Mahmoud");
        String receptionistToken = createUserAndLogin(
                "reception", Role.RECEPTIONIST, Set.of(SystemPermission.EMPLOYEES));

        mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/employees")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest("Blocked"))))
                .andExpect(status().isCreated());

        JsonNode dailyWage = createTransaction(
                receptionistToken,
                employeeId,
                EmployeeTransactionType.DAILY_WAGE,
                "250.00",
                null);

        CreateEmployeeTransactionRequest bonus = transactionRequest(
                employeeId, EmployeeTransactionType.BONUS, "10.00", null);
        mockMvc.perform(post("/api/v1/employee-transactions")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bonus)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/employee-transactions/{transactionId}/cancel",
                            dailyWage.path("id").asLong())
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CancelEmployeeTransactionRequest("Not permitted"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/employee-transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken)))
                .andExpect(status().isOk());
    }

    private long createEmployee(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/employees")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeRequest(name))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdBy").isNotEmpty())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions setEmployeeActive(
            String token,
            long employeeId,
            boolean active) throws Exception {
        return mockMvc.perform(patch("/api/v1/employees/{employeeId}/active", employeeId)
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EmployeeActiveRequest(active))));
    }

    private JsonNode createTransaction(
            String token,
            long employeeId,
            EmployeeTransactionType type,
            String amount,
            String paymentMethod) throws Exception {
        return createTransaction(token, employeeId, type, amount, paymentMethod, BUSINESS_DATE);
    }

    private JsonNode createTransaction(
            String token,
            long employeeId,
            EmployeeTransactionType type,
            String amount,
            String paymentMethod,
            LocalDate businessDate) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/employee-transactions")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                transactionRequest(employeeId, type, amount, paymentMethod, businessDate))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(employeeId))
                .andExpect(jsonPath("$.type").value(type.name()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static CreateEmployeeRequest employeeRequest(String name) {
        return new CreateEmployeeRequest(
                name, "01012345678", "Stylist", new BigDecimal("300.00"), null);
    }

    private static CreateEmployeeTransactionRequest transactionRequest(
            long employeeId,
            EmployeeTransactionType type,
            String amount,
            String paymentMethod) {
        return transactionRequest(employeeId, type, amount, paymentMethod, BUSINESS_DATE);
    }

    private static CreateEmployeeTransactionRequest transactionRequest(
            long employeeId,
            EmployeeTransactionType type,
            String amount,
            String paymentMethod,
            LocalDate businessDate) {
        return new CreateEmployeeTransactionRequest(
                employeeId,
                type,
                new BigDecimal(amount),
                businessDate,
                paymentMethod,
                "Test transaction");
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
        return loginAndReadToken(username);
    }

    private String loginAndReadToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("accessToken")
                .asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
