// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omar.beautycenter.auth.dto.LoginRequest;
import com.omar.beautycenter.catalog.dto.CatalogActiveRequest;
import com.omar.beautycenter.catalog.dto.CreateCatalogCategoryRequest;
import com.omar.beautycenter.catalog.dto.CreateCatalogItemRequest;
import com.omar.beautycenter.catalog.dto.CreateHairConfigurationRequest;
import com.omar.beautycenter.catalog.dto.HairCalculationPreviewRequest;
import com.omar.beautycenter.catalog.dto.UpdateCatalogPriceRequest;
import com.omar.beautycenter.discount.DiscountType;
import com.omar.beautycenter.discount.dto.CreateDiscountRequest;
import com.omar.beautycenter.role.Role;
import com.omar.beautycenter.user.User;
import com.omar.beautycenter.user.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogDiscountHairIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM hair_stock_movements");
        jdbcTemplate.update("DELETE FROM hair_product_stocks");
        jdbcTemplate.update("DELETE FROM hair_price_override_audits");
        jdbcTemplate.update("DELETE FROM hair_configurations");
        jdbcTemplate.update("DELETE FROM discounts");
        jdbcTemplate.update("DELETE FROM price_history");
        jdbcTemplate.update("DELETE FROM catalog_items");
        jdbcTemplate.update("DELETE FROM catalog_categories");
        userRepository.deleteAll();
    }

    @Test
    void catalogSupportsAllItemTypesAndReceptionistReadOnlyAccess() throws Exception {
        String managerToken = createUserAndLogin("admin", Role.ADMIN);
        long categoryId = createCategory(managerToken, true);
        for (CatalogItemType type : CatalogItemType.values()) {
            createItem(managerToken, categoryId, type.name(), type, "100.00", "40.00", true);
        }

        String receptionistToken = createUserAndLogin("reception", Role.RECEPTIONIST);
        mockMvc.perform(get("/api/v1/catalog/items")
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken))
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].currentCost").value(nullValue()));

        mockMvc.perform(post("/api/v1/catalog/categories")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest("Blocked", true))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/catalog/categories/{categoryId}/active", categoryId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CatalogActiveRequest(false))))
                .andExpect(status().isOk());
        performCreateItem(managerToken, categoryId, "BLOCKED", CatalogItemType.SERVICE, "1.00", "0.00", true)
                .andExpect(status().isConflict());
    }

    @Test
    void activeDiscountAndHairConfigurationSearchesSupportNullableFiltersAndPaging() throws Exception {
        String adminToken = createUserAndLogin("admin", Role.ADMIN);
        long categoryId = createCategory(adminToken, true);
        long hairItemId = createItem(
                adminToken, categoryId, "ACTIVE_HAIR", CatalogItemType.HAIR_PRODUCT, "50.00", "20.00", true);
        LocalDate effectiveDate = currentDate();
        long globalDiscountId = createDiscount(adminToken, discountRequest(
                "Global discount", DiscountType.PERCENTAGE, "10.00", null, null, true, effectiveDate, effectiveDate));
        long itemDiscountId = createDiscount(adminToken, discountRequest(
                "Item discount", DiscountType.PERCENTAGE, "10.00", hairItemId, null, true, effectiveDate, effectiveDate));
        long categoryDiscountId = createDiscount(adminToken, discountRequest(
                "Category discount", DiscountType.PERCENTAGE, "10.00", null, categoryId, true, effectiveDate, effectiveDate));
        long nullColorConfigurationId = createHairConfiguration(
                adminToken, hairItemId, "50.00", "20.00", "30.00", null, effectiveDate, true, null);
        createHairConfiguration(
                adminToken, hairItemId, "55.00", "20.00", "30.00", null, effectiveDate, true, "Black");

        mockMvc.perform(get("/api/v1/discounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("active", "true")
                        .param("applicableDate", effectiveDate.toString())
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].id").value(org.hamcrest.Matchers.hasItems(
                        (int) globalDiscountId,
                        (int) itemDiscountId,
                        (int) categoryDiscountId)));

        mockMvc.perform(get("/api/v1/discounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("active", "true")
                        .param("applicableDate", effectiveDate.toString())
                        .param("query", "")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/v1/discounts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("active", "true")
                        .param("applicableDate", effectiveDate.plusDays(1).toString())
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/hair-configurations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("active", "true")
                        .param("effectiveOn", effectiveDate.toString())
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value((int) nullColorConfigurationId))
                .andExpect(jsonPath("$.content[0].color").value(nullValue()));

        mockMvc.perform(get("/api/v1/hair-configurations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("active", "true")
                        .param("effectiveOn", effectiveDate.toString())
                        .param("query", "")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/hair-configurations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("active", "true")
                        .param("effectiveOn", effectiveDate.minusDays(1).toString())
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        String receptionistToken = createUserAndLogin("reception", Role.RECEPTIONIST);
        mockMvc.perform(get("/api/v1/hair-configurations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken))
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].unitCost").value(nullValue()));
    }

    @Test
    void priceChangesCreateImmutableHistoryAndPreserveEveryOldValue() throws Exception {
        String ownerToken = createUserAndLogin("owner", Role.OWNER);
        long categoryId = createCategory(ownerToken, true);
        long itemId = createItem(
                ownerToken, categoryId, "SERVICE_A", CatalogItemType.SERVICE, "100.00", "40.00", true);

        performPriceUpdate(ownerToken, itemId, "120.00", "50.00", "Supplier and market update")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentSellingPrice").value(120.00))
                .andExpect(jsonPath("$.currentCost").value(50.00));
        performPriceUpdate(ownerToken, itemId, "135.00", null, "Second selling-price update")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentSellingPrice").value(135.00));

        mockMvc.perform(get("/api/v1/price-history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("catalogItemId", String.valueOf(itemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        LocalDateTime reportFrom = LocalDateTime.of(currentDate().minusDays(1), LocalTime.MIN);
        LocalDateTime reportTo = LocalDateTime.of(currentDate(), LocalTime.MAX);
        mockMvc.perform(get("/api/v1/reports/price-history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("fromDateTime", reportFrom.toString())
                        .param("toDateTime", reportTo.toString())
                        .param("page", "0")
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(3));

        mockMvc.perform(get("/api/v1/reports/price-history")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                        .param("valueType", "COST")
                        .param("fromDateTime", reportFrom.toString())
                        .param("toDateTime", reportTo.toString())
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT value_type, old_value, new_value FROM price_history WHERE catalog_item_id = ? ORDER BY id",
                itemId);
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).get("VALUE_TYPE")).isEqualTo("SELLING_PRICE");
        assertMoney(rows.get(0).get("OLD_VALUE"), "100.00");
        assertMoney(rows.get(0).get("NEW_VALUE"), "120.00");
        assertThat(rows.get(1).get("VALUE_TYPE")).isEqualTo("COST");
        assertMoney(rows.get(1).get("OLD_VALUE"), "40.00");
        assertMoney(rows.get(1).get("NEW_VALUE"), "50.00");
        assertThat(rows.get(2).get("VALUE_TYPE")).isEqualTo("SELLING_PRICE");
        assertMoney(rows.get(2).get("OLD_VALUE"), "120.00");
        assertMoney(rows.get(2).get("NEW_VALUE"), "135.00");

        performPriceUpdate(ownerToken, itemId, "135.00", "50.00", "No actual change")
                .andExpect(status().isConflict());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM price_history WHERE catalog_item_id = ?",
                Integer.class,
                itemId)).isEqualTo(3);
    }

    @Test
    void percentageDiscountAcceptsZeroAndOneHundredButRejectsAboveBoundary() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long categoryId = createCategory(token, true);
        long itemId = createItem(token, categoryId, "HAIR", CatalogItemType.HAIR_PRODUCT, "50.00", "20.00", true);

        long zeroDiscountId = createDiscount(token, discountRequest(
                "Zero", DiscountType.PERCENTAGE, "0.00", itemId, null, true, currentDate(), currentDate()));
        long fullDiscountId = createDiscount(token, discountRequest(
                "Full", DiscountType.PERCENTAGE, "100.00", itemId, null, true, currentDate(), currentDate()));
        performCreateDiscount(token, discountRequest("Too much", DiscountType.PERCENTAGE, "100.01", itemId, null, true, currentDate(), currentDate()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Percentage discounts must be between 0 and 100."));
        performCreateDiscount(token, discountRequest("Ambiguous", DiscountType.FIXED_AMOUNT, "1.00", itemId, categoryId, true, currentDate(), currentDate()))
                .andExpect(status().isConflict());

        long configurationId = createHairConfiguration(
                token, itemId, "50.00", "20.00", "0.00", null, currentDate(), true);
        performPreview(token, new HairCalculationPreviewRequest(
                        configurationId, new BigDecimal("2.000"), zeroDiscountId, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountAmount").value(0.00))
                .andExpect(jsonPath("$.finalAmount").value(100.00));
        performPreview(token, new HairCalculationPreviewRequest(
                        configurationId, new BigDecimal("2.000"), fullDiscountId, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountAmount").value(100.00))
                .andExpect(jsonPath("$.finalAmount").value(0.00));
    }

    @Test
    void hairPreviewUsesIsolatedAssumedFormulaAndPercentageDiscount() throws Exception {
        String token = createUserAndLogin("admin", Role.ADMIN);
        long categoryId = createCategory(token, true);
        long itemId = createItem(token, categoryId, "HAIR_P", CatalogItemType.HAIR_PRODUCT, "0.00", "0.00", true);
        long configurationId = createHairConfiguration(token, itemId, "50.00", "20.00", "30.00", "1.000", currentDate(), true);
        long discountId = createDiscount(token, discountRequest(
                "Hair ten percent", DiscountType.PERCENTAGE, "10.00", itemId, null, true, currentDate(), currentDate()));

        performPreview(token, new HairCalculationPreviewRequest(
                        configurationId, new BigDecimal("3.000"), discountId, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseProductAmount").value(150.00))
                .andExpect(jsonPath("$.installationFee").value(30.00))
                .andExpect(jsonPath("$.subtotal").value(180.00))
                .andExpect(jsonPath("$.discountAmount").value(18.00))
                .andExpect(jsonPath("$.calculatedFinalAmount").value(162.00))
                .andExpect(jsonPath("$.finalAmount").value(162.00))
                .andExpect(jsonPath("$.costEstimate").value(60.00))
                .andExpect(jsonPath("$.overridden").value(false));
    }

    @Test
    void receptionistCannotReadCatalogOrHairCostValuesFromTheApi() throws Exception {
        String adminToken = createUserAndLogin("admin", Role.ADMIN);
        HairFixture fixture = createHairFixture(adminToken, "PRIVATE_COST_HAIR");
        String receptionistToken = createUserAndLogin("reception", Role.RECEPTIONIST);

        mockMvc.perform(get("/api/v1/hair-configurations/{configurationId}", fixture.configurationId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitCost").value(nullValue()));
        performPreview(receptionistToken, new HairCalculationPreviewRequest(
                        fixture.configurationId(), new BigDecimal("2.000"), null, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costEstimate").value(nullValue()));
    }

    @Test
    void fixedDiscountCannotMakeHairPriceNegative() throws Exception {
        String token = createUserAndLogin("admin", Role.ADMIN);
        HairFixture fixture = createHairFixture(token, "FIXED_HAIR");
        long validDiscountId = createDiscount(token, discountRequest(
                "Fixed 50", DiscountType.FIXED_AMOUNT, "50.00", null, fixture.categoryId(), true, currentDate(), currentDate()));
        long excessiveDiscountId = createDiscount(token, discountRequest(
                "Fixed 200", DiscountType.FIXED_AMOUNT, "200.00", null, null, true, currentDate(), currentDate()));

        performPreview(token, new HairCalculationPreviewRequest(
                        fixture.configurationId(), new BigDecimal("3.000"), validDiscountId, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(180.00))
                .andExpect(jsonPath("$.discountAmount").value(50.00))
                .andExpect(jsonPath("$.finalAmount").value(130.00));
        performPreview(token, new HairCalculationPreviewRequest(
                        fixture.configurationId(), new BigDecimal("3.000"), excessiveDiscountId, null, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A fixed discount cannot create a negative final price."));
    }

    @Test
    void expiredInactiveAndWrongScopeDiscountsCannotBeApplied() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        HairFixture fixture = createHairFixture(token, "SCOPE_HAIR");
        long otherItemId = createItem(
                token, fixture.categoryId(), "OTHER_HAIR", CatalogItemType.HAIR_PRODUCT, "0.00", "0.00", true);
        long expiredId = createDiscount(token, discountRequest(
                "Expired", DiscountType.PERCENTAGE, "10.00", null, null, true, currentDate().minusDays(2), currentDate().minusDays(1)));
        long inactiveId = createDiscount(token, discountRequest(
                "Inactive", DiscountType.PERCENTAGE, "10.00", null, null, false, currentDate(), currentDate())) ;
        long wrongScopeId = createDiscount(token, discountRequest(
                "Other item", DiscountType.PERCENTAGE, "10.00", otherItemId, null, true, currentDate(), currentDate()));

        for (long discountId : List.of(expiredId, inactiveId, wrongScopeId)) {
            performPreview(token, new HairCalculationPreviewRequest(
                            fixture.configurationId(), new BigDecimal("3.000"), discountId, null, null))
                    .andExpect(status().isConflict());
        }
    }

    @Test
    void hairConfigurationEnforcesHairTypeMinimumEffectiveDateAndActiveState() throws Exception {
        String token = createUserAndLogin("owner", Role.OWNER);
        long categoryId = createCategory(token, true);
        long serviceId = createItem(token, categoryId, "NOT_HAIR", CatalogItemType.SERVICE, "20.00", "5.00", true);
        performCreateHairConfiguration(token, serviceId, "50.00", "20.00", "30.00", "1.000", currentDate(), true)
                .andExpect(status().isConflict());

        long itemId = createItem(token, categoryId, "VALID_HAIR", CatalogItemType.HAIR_PRODUCT, "0.00", "0.00", true);
        long minimumConfigurationId = createHairConfiguration(
                token, itemId, "50.00", "20.00", "30.00", "2.000", currentDate(), true);
        performPreview(token, new HairCalculationPreviewRequest(
                        minimumConfigurationId, new BigDecimal("1.000"), null, null, null))
                .andExpect(status().isConflict());

        long futureConfigurationId = createHairConfiguration(
                token, itemId, "50.00", "20.00", "30.00", null, currentDate().plusDays(1), true);
        performPreview(token, new HairCalculationPreviewRequest(
                        futureConfigurationId, new BigDecimal("1.000"), null, null, null))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/hair-configurations/{configurationId}/active", minimumConfigurationId)
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CatalogActiveRequest(false))))
                .andExpect(status().isOk());
        performPreview(token, new HairCalculationPreviewRequest(
                        minimumConfigurationId, new BigDecimal("2.000"), null, null, null))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyOwnerOrAdminCanOverrideAndEveryOverrideIsAudited() throws Exception {
        String managerToken = createUserAndLogin("admin", Role.ADMIN);
        HairFixture fixture = createHairFixture(managerToken, "OVERRIDE_HAIR");
        String receptionistToken = createUserAndLogin("reception", Role.RECEPTIONIST);
        HairCalculationPreviewRequest overrideRequest = new HairCalculationPreviewRequest(
                fixture.configurationId(),
                new BigDecimal("3.000"),
                null,
                new BigDecimal("140.00"),
                "Authorized negotiated price");

        performPreview(receptionistToken, overrideRequest)
                .andExpect(status().isForbidden());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hair_price_override_audits",
                Integer.class)).isZero();

        performPreview(managerToken, new HairCalculationPreviewRequest(
                        fixture.configurationId(),
                        new BigDecimal("3.000"),
                        null,
                        new BigDecimal("140.00"),
                        null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Override reason must contain 1 to 500 characters."));

        performPreview(managerToken, overrideRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculatedFinalAmount").value(180.00))
                .andExpect(jsonPath("$.overridden").value(true))
                .andExpect(jsonPath("$.finalAmount").value(140.00));

        mockMvc.perform(get("/api/v1/hair-configurations/price-overrides")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .param("configurationId", String.valueOf(fixture.configurationId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].originalAmount").value(180.00))
                .andExpect(jsonPath("$.content[0].overriddenAmount").value(140.00))
                .andExpect(jsonPath("$.content[0].authorizedBy").value("admin"))
                .andExpect(jsonPath("$.content[0].overrideReason").value("Authorized negotiated price"));

        mockMvc.perform(get("/api/v1/hair-configurations/price-overrides")
                        .header(HttpHeaders.AUTHORIZATION, bearer(receptionistToken)))
                .andExpect(status().isForbidden());
    }

    private HairFixture createHairFixture(String token, String code) throws Exception {
        long categoryId = createCategory(token, true);
        long itemId = createItem(token, categoryId, code, CatalogItemType.HAIR_PRODUCT, "0.00", "0.00", true);
        long configurationId = createHairConfiguration(
                token, itemId, "50.00", "20.00", "30.00", "1.000", currentDate(), true);
        return new HairFixture(categoryId, itemId, configurationId);
    }

    private long createCategory(String token, boolean active) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/catalog/categories")
                        .with(csrf())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(categoryRequest("Category", active))))
                .andExpect(status().isCreated())
                .andReturn();
        return readId(result);
    }

    private static CreateCatalogCategoryRequest categoryRequest(String suffix, boolean active) {
        return new CreateCatalogCategoryRequest(
                "فئة " + suffix,
                "Catalog " + suffix,
                active,
                "Test category");
    }

    private long createItem(
            String token,
            long categoryId,
            String code,
            CatalogItemType type,
            String sellingPrice,
            String cost,
            boolean active) throws Exception {
        return readId(performCreateItem(token, categoryId, code, type, sellingPrice, cost, active)
                .andExpect(status().isCreated())
                .andReturn());
    }

    private ResultActions performCreateItem(
            String token,
            long categoryId,
            String code,
            CatalogItemType type,
            String sellingPrice,
            String cost,
            boolean active) throws Exception {
        CreateCatalogItemRequest request = new CreateCatalogItemRequest(
                code,
                "عنصر " + code,
                "Item " + code,
                type,
                categoryId,
                new BigDecimal(sellingPrice),
                new BigDecimal(cost),
                active,
                "Test item");
        return mockMvc.perform(post("/api/v1/catalog/items")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions performPriceUpdate(
            String token,
            long itemId,
            String sellingPrice,
            String cost,
            String reason) throws Exception {
        UpdateCatalogPriceRequest request = new UpdateCatalogPriceRequest(
                sellingPrice == null ? null : new BigDecimal(sellingPrice),
                cost == null ? null : new BigDecimal(cost),
                reason);
        return mockMvc.perform(post("/api/v1/catalog/items/{itemId}/prices", itemId)
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private long createDiscount(String token, CreateDiscountRequest request) throws Exception {
        return readId(performCreateDiscount(token, request)
                .andExpect(status().isCreated())
                .andReturn());
    }

    private ResultActions performCreateDiscount(String token, CreateDiscountRequest request) throws Exception {
        return mockMvc.perform(post("/api/v1/discounts")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private static CreateDiscountRequest discountRequest(
            String name,
            DiscountType type,
            String value,
            Long itemId,
            Long categoryId,
            boolean active,
            LocalDate startDate,
            LocalDate endDate) {
        return new CreateDiscountRequest(
                name,
                type,
                new BigDecimal(value),
                startDate,
                endDate,
                active,
                itemId,
                categoryId);
    }

    private long createHairConfiguration(
            String token,
            long itemId,
            String unitPrice,
            String unitCost,
            String installationFee,
            String minimumQuantity,
            LocalDate effectiveDate,
            boolean active) throws Exception {
        return createHairConfiguration(
                token,
                itemId,
                unitPrice,
                unitCost,
                installationFee,
                minimumQuantity,
                effectiveDate,
                active,
                "Black");
    }

    private long createHairConfiguration(
            String token,
            long itemId,
            String unitPrice,
            String unitCost,
            String installationFee,
            String minimumQuantity,
            LocalDate effectiveDate,
            boolean active,
            String color) throws Exception {
        return readId(performCreateHairConfiguration(
                        token,
                        itemId,
                        unitPrice,
                        unitCost,
                        installationFee,
                        minimumQuantity,
                        effectiveDate,
                        active,
                        color)
                .andExpect(status().isCreated())
                .andReturn());
    }

    private ResultActions performCreateHairConfiguration(
            String token,
            long itemId,
            String unitPrice,
            String unitCost,
            String installationFee,
            String minimumQuantity,
            LocalDate effectiveDate,
            boolean active) throws Exception {
        return performCreateHairConfiguration(
                token,
                itemId,
                unitPrice,
                unitCost,
                installationFee,
                minimumQuantity,
                effectiveDate,
                active,
                "Black");
    }

    private ResultActions performCreateHairConfiguration(
            String token,
            long itemId,
            String unitPrice,
            String unitCost,
            String installationFee,
            String minimumQuantity,
            LocalDate effectiveDate,
            boolean active,
            String color) throws Exception {
        CreateHairConfigurationRequest request = new CreateHairConfigurationRequest(
                itemId,
                "Test brand",
                "50 cm",
                color,
                "bundle",
                new BigDecimal(unitPrice),
                new BigDecimal(unitCost),
                new BigDecimal(installationFee),
                minimumQuantity == null ? null : new BigDecimal(minimumQuantity),
                active,
                effectiveDate);
        return mockMvc.perform(post("/api/v1/hair-configurations")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions performPreview(String token, HairCalculationPreviewRequest request) throws Exception {
        return mockMvc.perform(post("/api/v1/hair-configurations/preview")
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private LocalDate currentDate() {
        return LocalDate.now(clock);
    }

    private String createUserAndLogin(String username, Role role) throws Exception {
        User user = new User(
                username,
                "Test " + role.name(),
                passwordEncoder.encode(PASSWORD),
                true,
                Set.of(role));
        userRepository.saveAndFlush(user);
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

    private long readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("id").asLong();
    }

    private static void assertMoney(Object actual, String expected) {
        assertThat(new BigDecimal(actual.toString())).isEqualByComparingTo(expected);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record HairFixture(long categoryId, long itemId, long configurationId) {
    }
}
