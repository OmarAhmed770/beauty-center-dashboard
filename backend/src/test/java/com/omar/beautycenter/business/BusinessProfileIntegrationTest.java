// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.business;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omar.beautycenter.auth.dto.LoginRequest;
import com.omar.beautycenter.business.dto.UpdateBusinessProfileRequest;
import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.User;
import com.omar.beautycenter.user.UserRepository;
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
class BusinessProfileIntegrationTest {

    // TEST ONLY - synthetic password.
    private static final String PASSWORD = "test-only-strong-password";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetState() {
        userRepository.deleteAll();
        jdbcTemplate.update("""
                UPDATE business_profile
                SET business_name = 'مركز التجميل', logo_data_url = NULL, phone = NULL, address = NULL,
                    currency = 'EGP', receipt_information = 'شكرًا لزيارتكم',
                    updated_at = CURRENT_TIMESTAMP, updated_by = 'SYSTEM'
                WHERE id = 1
                """);
    }

    @Test
    void profileIsAvailableToEveryAuthenticatedRoleButOnlyOwnerCanChangeIt() throws Exception {
        String receptionistToken = createUserAndLogin("reception-profile", Role.RECEPTIONIST);
        mockMvc.perform(get("/api/v1/business-profile").header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("مركز التجميل"))
                .andExpect(jsonPath("$.currency").value("EGP"));

        String adminToken = createUserAndLogin("admin-profile", Role.ADMIN);
        mockMvc.perform(put("/api/v1/business-profile")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(BusinessCurrency.SAR, null))))
                .andExpect(status().isForbidden());

        String ownerToken = createUserAndLogin("owner-profile", Role.OWNER);
        mockMvc.perform(put("/api/v1/business-profile")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(
                                BusinessCurrency.SAR,
                                "data:image/png;base64,iVBORw0KGgo="))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("صالون الاختبار"))
                .andExpect(jsonPath("$.currency").value("SAR"))
                .andExpect(jsonPath("$.logoDataUrl").value("data:image/png;base64,iVBORw0KGgo="));

        mockMvc.perform(get("/api/v1/business-profile").header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessName").value("صالون الاختبار"))
                .andExpect(jsonPath("$.currency").value("SAR"));
    }

    @Test
    void ownerCannotStoreAnUnsafeLogoPayload() throws Exception {
        String ownerToken = createUserAndLogin("owner-logo", Role.OWNER);
        mockMvc.perform(put("/api/v1/business-profile")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(
                                BusinessCurrency.EGP,
                                "data:text/html;base64,PHNjcmlwdD4="))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Logo must be a PNG, JPEG, or WebP image."));
    }

    private UpdateBusinessProfileRequest request(BusinessCurrency currency, String logoDataUrl) {
        return new UpdateBusinessProfileRequest(
                "صالون الاختبار", logoDataUrl, "01000000000", "القاهرة", currency, "شكرًا لزيارتكم");
    }

    private String createUserAndLogin(String username, Role role) throws Exception {
        userRepository.saveAndFlush(new User(
                username, "Test " + role.name(), passwordEncoder.encode(PASSWORD), true, Set.of(role)));
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
