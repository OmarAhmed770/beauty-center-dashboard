// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omar.beautycenter.auth.dto.LoginRequest;
import com.omar.beautycenter.closing.dto.CloseDailyRequest;
import com.omar.beautycenter.closing.dto.ReopenDailyRequest;
import com.omar.beautycenter.employee.dto.CreateEmployeeRequest;
import com.omar.beautycenter.employee.dto.CreateEmployeeTransactionRequest;
import com.omar.beautycenter.employee.dto.SaveEmployeeAttendanceRequest;
import com.omar.beautycenter.role.Role;
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
class EmployeeAttendanceIntegrationTest {

    // TEST ONLY - synthetic password.
    private static final String PASSWORD = "test-only-strong-password";
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2027, 2, 3);

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM daily_closing_payment_totals");
        jdbcTemplate.update("DELETE FROM daily_closing_expense_totals");
        jdbcTemplate.update("DELETE FROM daily_closings");
        jdbcTemplate.update("DELETE FROM employee_attendances");
        jdbcTemplate.update("DELETE FROM employee_transactions");
        jdbcTemplate.update("DELETE FROM employees");
        jdbcTemplate.update("DELETE FROM audit_logs");
        userRepository.deleteAll();
    }

    @Test
    void presentAttendanceCreatesExactlyOneWageAndAbsentReversesAnUnpaidWage() throws Exception {
        String ownerToken = createUserAndLogin("attendance-owner", Role.OWNER);
        long employeeId = createEmployee(ownerToken, "Mona Attendance", "300.00");

        mockMvc.perform(get("/api/v1/employees/attendance")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("businessDate", BUSINESS_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].employeeId").value(employeeId))
                .andExpect(jsonPath("$[0].dailyWageAmount").value(300.00));

        JsonNode first = saveAttendance(ownerToken, employeeId, EmployeeAttendanceStatus.PRESENT);
        JsonNode repeated = saveAttendance(ownerToken, employeeId, EmployeeAttendanceStatus.PRESENT);

        assertThat(first.path("dailyWageTransactionId").asLong())
                .isEqualTo(repeated.path("dailyWageTransactionId").asLong());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee_transactions WHERE employee_id = ? AND transaction_type = 'DAILY_WAGE'",
                Integer.class, employeeId)).isEqualTo(1);

        mockMvc.perform(get("/api/v1/employees/{employeeId}/totals", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyWages").value(300.00))
                .andExpect(jsonPath("$.netEntitlement").value(300.00));

        mockMvc.perform(get("/api/v1/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("filter", "CUSTOM")
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyWages").value(300.00))
                .andExpect(jsonPath("$.employeeCost").value(300.00))
                .andExpect(jsonPath("$.netProfit").value(-300.00));

        saveAttendance(ownerToken, employeeId, EmployeeAttendanceStatus.ABSENT)
                .path("dailyWageTransactionStatus").asText();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee_transactions WHERE employee_id = ? AND status = 'ACTIVE'",
                Integer.class, employeeId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'EMPLOYEE_ATTENDANCE_WAGE_REVERSED'",
                Integer.class)).isEqualTo(1);

        mockMvc.perform(get("/api/v1/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("filter", "CUSTOM")
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyWages").value(0.00))
                .andExpect(jsonPath("$.employeeCost").value(0.00));
    }

    @Test
    void absentAttendanceCreatesNoWageEntitlement() throws Exception {
        String ownerToken = createUserAndLogin("attendance-absent-owner", Role.OWNER);
        long employeeId = createEmployee(ownerToken, "Absent Employee", "300.00");

        saveAttendance(ownerToken, employeeId, EmployeeAttendanceStatus.ABSENT);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee_transactions WHERE employee_id = ?", Integer.class, employeeId))
                .isZero();
    }

    @Test
    void paidAttendanceWageCannotBeSilentlyReversedAndPaymentsAffectCashOnlyWhenCash() throws Exception {
        String ownerToken = createUserAndLogin("attendance-payments-owner", Role.OWNER);
        long cashEmployee = createEmployee(ownerToken, "Cash Wage", "200.00");
        long cardEmployee = createEmployee(ownerToken, "Card Wage", "100.00");
        saveAttendance(ownerToken, cashEmployee, EmployeeAttendanceStatus.PRESENT);
        saveAttendance(ownerToken, cardEmployee, EmployeeAttendanceStatus.PRESENT);

        createTransaction(ownerToken, cashEmployee, EmployeeTransactionType.PAYMENT, "50.00", "CASH");
        createTransaction(ownerToken, cardEmployee, EmployeeTransactionType.PAYMENT, "100.00", "VISA");

        mockMvc.perform(put("/api/v1/employees/{employeeId}/attendance", cashEmployee)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("businessDate", BUSINESS_DATE.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SaveEmployeeAttendanceRequest(EmployeeAttendanceStatus.ABSENT))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "This attendance wage may already be settled. Correct the employee payment before marking attendance absent."));

        mockMvc.perform(get("/api/v1/employees/{employeeId}/totals", cashEmployee)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyWages").value(200.00))
                .andExpect(jsonPath("$.payments").value(50.00))
                .andExpect(jsonPath("$.netEntitlement").value(150.00));

        mockMvc.perform(get("/api/v1/employees/{employeeId}/totals", cardEmployee)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyWages").value(100.00))
                .andExpect(jsonPath("$.payments").value(100.00))
                .andExpect(jsonPath("$.netEntitlement").value(0.00));

        mockMvc.perform(get("/api/v1/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("filter", "CUSTOM")
                        .param("fromDate", BUSINESS_DATE.toString())
                        .param("toDate", BUSINESS_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeCost").value(300.00))
                .andExpect(jsonPath("$.cashEmployeePayments").value(50.00))
                .andExpect(jsonPath("$.expectedCashBalance").value(-50.00));
    }

    @Test
    void onlyOwnerCanRecordAttendance() throws Exception {
        String ownerToken = createUserAndLogin("attendance-access-owner", Role.OWNER);
        long employeeId = createEmployee(ownerToken, "Access Employee", "200.00");
        String adminToken = createUserAndLogin("attendance-access-admin", Role.ADMIN);
        String receptionistToken = createUserAndLogin("attendance-access-reception", Role.RECEPTIONIST);

        for (String token : Set.of(adminToken, receptionistToken)) {
            mockMvc.perform(get("/api/v1/employees/attendance")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .param("businessDate", BUSINESS_DATE.toString()))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/api/v1/employees/{employeeId}/attendance", employeeId)
                            .with(csrf())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .param("businessDate", BUSINESS_DATE.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new SaveEmployeeAttendanceRequest(EmployeeAttendanceStatus.PRESENT))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void recloseKeepsTheOldAttendanceWageSnapshotAndUsesTheNewOne() throws Exception {
        String ownerToken = createUserAndLogin("attendance-closing-owner", Role.OWNER);
        long firstEmployee = createEmployee(ownerToken, "First Closing Employee", "300.00");
        long secondEmployee = createEmployee(ownerToken, "Second Closing Employee", "200.00");
        saveAttendance(ownerToken, firstEmployee, EmployeeAttendanceStatus.PRESENT);

        mockMvc.perform(post("/api/v1/daily-closings/{date}/close", BUSINESS_DATE)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CloseDailyRequest(BigDecimal.ZERO, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyWages").value(300.00));

        mockMvc.perform(put("/api/v1/employees/{employeeId}/attendance", secondEmployee)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("businessDate", BUSINESS_DATE.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SaveEmployeeAttendanceRequest(EmployeeAttendanceStatus.PRESENT))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/daily-closings/{date}/reopen", BUSINESS_DATE)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReopenDailyRequest("Attendance correction"))))
                .andExpect(status().isOk());
        saveAttendance(ownerToken, secondEmployee, EmployeeAttendanceStatus.PRESENT);

        mockMvc.perform(post("/api/v1/daily-closings/{date}/close", BUSINESS_DATE)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CloseDailyRequest(BigDecimal.ZERO, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyWages").value(500.00))
                .andExpect(jsonPath("$.closingVersion").value(2));

        mockMvc.perform(get("/api/v1/daily-closings/{date}/versions", BUSINESS_DATE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dailyWages").value(500.00))
                .andExpect(jsonPath("$[1].dailyWages").value(300.00));
    }

    private JsonNode saveAttendance(String token, long employeeId, EmployeeAttendanceStatus statusValue) throws Exception {
        MvcResult result = mockMvc.perform(put("/api/v1/employees/{employeeId}/attendance", employeeId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("businessDate", BUSINESS_DATE.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SaveEmployeeAttendanceRequest(statusValue))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long createEmployee(String token, String name, String wage) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/employees")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateEmployeeRequest(
                                name, null, "Stylist", new BigDecimal(wage), null))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private void createTransaction(
            String token,
            long employeeId,
            EmployeeTransactionType type,
            String amount,
            String paymentMethod) throws Exception {
        mockMvc.perform(post("/api/v1/employee-transactions")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateEmployeeTransactionRequest(
                                employeeId, type, new BigDecimal(amount), BUSINESS_DATE, paymentMethod, null))))
                .andExpect(status().isCreated());
    }

    private String createUserAndLogin(String username, Role role) throws Exception {
        userRepository.saveAndFlush(new User(
                username,
                "Test " + role.name(),
                passwordEncoder.encode(PASSWORD),
                true,
                Set.of(role)));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
