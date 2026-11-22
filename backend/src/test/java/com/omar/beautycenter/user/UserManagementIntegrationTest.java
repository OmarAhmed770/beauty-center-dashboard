// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.user;

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
import com.omar.beautycenter.auth.StaffRegistrationRateLimiter;
import com.omar.beautycenter.auth.dto.StaffRegistrationRequest;
import com.omar.beautycenter.business.BusinessCurrency;
import com.omar.beautycenter.business.BusinessProfileRepository;
import com.omar.beautycenter.business.dto.UpdateBusinessProfileRequest;
import com.omar.beautycenter.catalog.CatalogItemRepository;
import com.omar.beautycenter.employee.Employee;
import com.omar.beautycenter.employee.EmployeeRepository;
import com.omar.beautycenter.expense.ExpenseCategoryRepository;
import com.omar.beautycenter.payment.PaymentMethodRepository;
import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.setup.StarterServiceCategory;
import com.omar.beautycenter.setup.dto.CreateInitialOwnerRequest;
import com.omar.beautycenter.setup.dto.StarterServiceRequest;
import com.omar.beautycenter.user.dto.CreateUserRequest;
import com.omar.beautycenter.user.dto.ApproveRegistrationRequest;
import com.omar.beautycenter.user.dto.ResetUserPasswordRequest;
import com.omar.beautycenter.user.dto.SetUserActiveRequest;
import com.omar.beautycenter.user.dto.UpdateUserRequest;
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
class UserManagementIntegrationTest {

    // TEST ONLY - synthetic password.
    private static final String PASSWORD = "test-only-strong-password";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private BusinessProfileRepository businessProfileRepository;
    @Autowired private CatalogItemRepository catalogItemRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private ExpenseCategoryRepository expenseCategoryRepository;
    @Autowired private PaymentMethodRepository paymentMethodRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private StaffRegistrationRateLimiter registrationRateLimiter;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
        jdbcTemplate.update("UPDATE app_setup_state SET initial_owner_setup_completed = FALSE WHERE id = 1");
        registrationRateLimiter.clear();
    }

    @Test
    void setupStatusRequiresIncompleteSingletonAndNoUsersAndFailsClosedWhenStateIsMissing() throws Exception {
        try {
            mockMvc.perform(get("/api/v1/setup/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.initialOwnerSetupRequired").value(true));

            jdbcTemplate.update("UPDATE app_setup_state SET initial_owner_setup_completed = TRUE WHERE id = 1");
            mockMvc.perform(get("/api/v1/setup/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.initialOwnerSetupRequired").value(false));

            jdbcTemplate.update("UPDATE app_setup_state SET initial_owner_setup_completed = FALSE WHERE id = 1");
            save("existing.setup.staff", Role.RECEPTIONIST, PASSWORD);
            mockMvc.perform(get("/api/v1/setup/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.initialOwnerSetupRequired").value(false));

            userRepository.deleteAll();
            jdbcTemplate.update("DELETE FROM app_setup_state WHERE id = 1");
            mockMvc.perform(get("/api/v1/setup/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.initialOwnerSetupRequired").value(false));
        } finally {
            // Restore the H2 test fixture even if an assertion fails; other tests expect this singleton.
            userRepository.deleteAll();
            if (jdbcTemplate.update("UPDATE app_setup_state SET initial_owner_setup_completed = FALSE WHERE id = 1") == 0) {
                jdbcTemplate.update("INSERT INTO app_setup_state (id, initial_owner_setup_completed) VALUES (1, FALSE)");
            }
        }
    }

    @Test
    void firstRunSetupCreatesOneOwnerAndCannotBeRepeated() throws Exception {
        mockMvc.perform(get("/api/v1/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialOwnerSetupRequired").value(true));

        mockMvc.perform(post("/api/v1/setup/owner")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateInitialOwnerRequest(
                                "first.owner", "First Owner", PASSWORD,
                                "مركز اختبار الإعداد", BusinessCurrency.SAR,
                                java.util.List.of(new StarterServiceRequest("قص شعر", StarterServiceCategory.HAIR)),
                                true, true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("first.owner"))
                .andExpect(jsonPath("$.roles[0]").value("OWNER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        org.junit.jupiter.api.Assertions.assertEquals(
                "مركز اختبار الإعداد", businessProfileRepository.findById(1L).orElseThrow().getBusinessName());
        org.junit.jupiter.api.Assertions.assertEquals(
                BusinessCurrency.SAR, businessProfileRepository.findById(1L).orElseThrow().getCurrency());
        org.junit.jupiter.api.Assertions.assertTrue(catalogItemRepository.findAll().stream()
                .anyMatch(item -> item.getNameArabic().equals("قص شعر")
                        && !item.isActive()
                        && item.getCurrentSellingPrice().signum() == 0));
        org.junit.jupiter.api.Assertions.assertTrue(expenseCategoryRepository.existsByNameArabicIgnoreCase("الإيجار"));
        org.junit.jupiter.api.Assertions.assertTrue(paymentMethodRepository.existsByCode("CASH"));

        mockMvc.perform(get("/api/v1/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialOwnerSetupRequired").value(false));

        mockMvc.perform(post("/api/v1/setup/owner")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateInitialOwnerRequest(
                                "second.owner", "Second Owner", PASSWORD))))
                .andExpect(status().isConflict());
    }

    @Test
    void ownerManagesUsersWhileFullyPermittedStaffCannotAccessOwnerOperations() throws Exception {
        String ownerToken = saveAndLogin("owner", Role.OWNER, PASSWORD);
        User existingStaff = save("staff.user", Role.ADMIN, PASSWORD);
        long staffId = existingStaff.getId();
        String staffToken = login("staff.user", PASSWORD);

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(staffToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(staffToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserRequest(
                                "blocked.user", "Blocked", PASSWORD, Set.of(Role.RECEPTIONIST)))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/users/{id}", staffId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRequest(
                                "Updated Staff", Set.of(Role.RECEPTIONIST)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Staff"));

        mockMvc.perform(post("/api/v1/users/{id}/password-reset", staffId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetUserPasswordRequest("new-test-only-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("staff.user", PASSWORD))))
                .andExpect(status().isUnauthorized());
        login("staff.user", "new-test-only-password");
    }

    @Test
    void ownerCannotDeactivateOrDemoteTheLastActiveOwner() throws Exception {
        User owner = save("owner", Role.OWNER, PASSWORD);
        String ownerToken = login("owner", PASSWORD);

        mockMvc.perform(patch("/api/v1/users/{id}/active", owner.getId())
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SetUserActiveRequest(false))))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/users/{id}", owner.getId())
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRequest(
                                "Owner", Set.of(Role.ADMIN)))))
                .andExpect(status().isConflict());
    }

    @Test
    void staffCannotGrantTheirOwnPermissionsEvenWithAllAssignedPermissions() throws Exception {
        Set<SystemPermission> allPermissions = java.util.EnumSet.allOf(SystemPermission.class);
        for (Set<SystemPermission> assigned : java.util.List.of(allPermissions, Set.of(SystemPermission.ADD_ORDERS))) {
            Employee employee = employeeRepository.saveAndFlush(new Employee(
                    "Permission Test Employee", null, "Stylist", java.math.BigDecimal.ZERO, null));
            User staff = new User("permission.staff." + assigned.size(), "Permission Test Staff",
                    passwordEncoder.encode(PASSWORD), true, Set.of(Role.RECEPTIONIST));
            staff.linkEmployee(employee);
            staff.setPermissions(assigned);
            userRepository.saveAndFlush(staff);
            String token = login(staff.getUsername(), PASSWORD);
            mockMvc.perform(put("/api/v1/employees/{id}/permissions", employee.getId())
                            .with(csrf())
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(java.util.Map.of("permissions", allPermissions))))
                    .andExpect(status().isForbidden());
            org.junit.jupiter.api.Assertions.assertEquals(assigned.size(),
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_permissions WHERE user_id = ?", Integer.class, staff.getId()));
        }
    }

    @Test
    void repeatedInvalidLoginsTemporarilyLockAnAccountWithoutChangingThePublicError() throws Exception {
        save("reception", Role.RECEPTIONIST, PASSWORD);
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest("reception", "wrong-password"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid username or password."));
        }
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("reception", PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void staffRegistrationStaysPendingUntilOnlyOwnerApprovesIt() throws Exception {
        String ownerToken = saveAndLogin("owner", Role.OWNER, PASSWORD);
        String adminToken = saveAndLogin("admin", Role.ADMIN, PASSWORD);

        MvcResult created = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffRegistrationRequest(
                                "Pending Receptionist", "pending.reception", "staff-password-2026", "staff-password-2026", "01000000000"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("pending.reception"))
                .andExpect(jsonPath("$.registrationStatus").value("PENDING"))
                .andReturn();

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("pending.reception", "staff-password-2026"))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/pending-registrations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isForbidden());

        MvcResult pending = mockMvc.perform(get("/api/v1/users/pending-registrations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].phone").value("01000000000"))
                .andExpect(jsonPath("$[0].roles").isEmpty())
                .andReturn();
        long pendingId = objectMapper.readTree(pending.getResponse().getContentAsString()).get(0).path("id").asLong();

        mockMvc.perform(patch("/api/v1/users/{id}/registration/approve", pendingId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRegistrationRequest(Role.RECEPTIONIST))))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/users/{id}/registration/approve", pendingId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRegistrationRequest(Role.ADMIN))))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/v1/users/{id}/registration/approve", pendingId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRegistrationRequest(Role.RECEPTIONIST))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.registrationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.roles[0]").value("RECEPTIONIST"))
                .andExpect(jsonPath("$.employeeId").isNumber())
                .andExpect(jsonPath("$.employeeName").value("Pending Receptionist"));
        login("pending.reception", "staff-password-2026");

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffRegistrationRequest(
                                "Rejected Staff", "rejected.staff", "staff-password-2026", "staff-password-2026", null))))
                .andExpect(status().isCreated());
        MvcResult rejectedPending = mockMvc.perform(get("/api/v1/users/pending-registrations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn();
        long rejectedId = objectMapper.readTree(rejectedPending.getResponse().getContentAsString()).get(0).path("id").asLong();
        mockMvc.perform(patch("/api/v1/users/{id}/registration/reject", rejectedId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.active").value(false));
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("rejected.staff", "staff-password-2026"))))
                .andExpect(status().isUnauthorized());

        JsonNode response = objectMapper.readTree(created.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertFalse(response.has("passwordHash"));
    }

    @Test
    void registrationRejectsWeakAndDuplicateRequestsAndRateLimitsSubmissions() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffRegistrationRequest(
                                "Weak Password", "weak.password", "letters-only-password", "letters-only-password", null))))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffRegistrationRequest(
                                "First Request", "duplicate.staff", "staff-password-2026", "staff-password-2026", null))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffRegistrationRequest(
                                "Duplicate Request", "duplicate.staff", "staff-password-2026", "staff-password-2026", null))))
                .andExpect(status().isConflict());

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new StaffRegistrationRequest(
                                    "Rate Limited " + attempt, "rate." + attempt, "staff-password-2026", "staff-password-2026", null))))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StaffRegistrationRequest(
                                "Rate Limited Final", "rate.final", "staff-password-2026", "staff-password-2026", null))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("REGISTRATION_RATE_LIMITED"));
    }

    @Test
    void ordinaryUserManagementCannotCreateAnotherOwner() throws Exception {
        String ownerToken = saveAndLogin("owner", Role.OWNER, PASSWORD);
        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserRequest(
                                "another.owner", "Another Owner", PASSWORD, Set.of(Role.OWNER)))))
                .andExpect(status().isConflict());
    }

    @Test
    void receptionistCannotCallOwnerOnlyEndpoints() throws Exception {
        String receptionistToken = saveAndLogin("reception-owner-only", Role.RECEPTIONIST, PASSWORD);

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/business-profile")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateBusinessProfileRequest(
                                "مركز الاختبار", null, null, null, BusinessCurrency.EGP, null))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/backups/manual")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/daily-closings/2026-01-01/reopen")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"اختبار\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void newStaffCanBeLinkedToAnExistingEmployeeOrLeftUnlinked() throws Exception {
        String ownerToken = saveAndLogin("owner-link", Role.OWNER, PASSWORD);
        Employee employee = employeeRepository.saveAndFlush(new Employee(
                "Existing Stylist", null, "Stylist", new java.math.BigDecimal("0.00"), null));

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"staff-linked","displayName":"Linked Staff","password":"test-only-strong-password","roles":["RECEPTIONIST"],"employeeLinkMode":"EXISTING","employeeId":%d}
                                """.formatted(employee.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(employee.getId()))
                .andExpect(jsonPath("$.employeeName").value("Existing Stylist"));

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"staff-unlinked","displayName":"Unlinked Staff","password":"test-only-strong-password","roles":["RECEPTIONIST"],"employeeLinkMode":"NONE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").doesNotExist());
    }

    private User save(String username, Role role, String password) {
        return userRepository.saveAndFlush(new User(
                username, "Test User", passwordEncoder.encode(password), true, Set.of(role)));
    }

    private String saveAndLogin(String username, Role role, String password) throws Exception {
        save(username, role, password);
        return login(username, password);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
