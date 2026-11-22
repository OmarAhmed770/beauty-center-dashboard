package com.omar.beautycenter.catalog;

import com.omar.beautycenter.catalog.dto.CreateHairConfigurationRequest;
import com.omar.beautycenter.catalog.dto.HairConfigurationResponse;
import com.omar.beautycenter.catalog.dto.HairPriceOverrideAuditResponse;
import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HairConfigurationService {

    private final HairConfigurationRepository configurationRepository;
    private final HairPriceOverrideAuditRepository overrideAuditRepository;
    private final CatalogItemService itemService;
    private final HairStockService hairStockService;

    public HairConfigurationService(
            HairConfigurationRepository configurationRepository,
            HairPriceOverrideAuditRepository overrideAuditRepository,
            CatalogItemService itemService,
            HairStockService hairStockService) {
        this.configurationRepository = configurationRepository;
        this.overrideAuditRepository = overrideAuditRepository;
        this.itemService = itemService;
        this.hairStockService = hairStockService;
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SERVICES_AND_PRICES')")
    public HairConfigurationResponse create(CreateHairConfigurationRequest request) {
        CatalogItem hairItem = getActiveHairItem(request.hairItemId());
        if (request.effectiveDate() == null) {
            throw new BusinessRuleException("Effective date is required.");
        }
        HairConfiguration configuration = new HairConfiguration(
                hairItem,
                CatalogValidation.requiredText(request.brand(), "Brand", 150),
                CatalogValidation.requiredText(request.length(), "Length", 100),
                CatalogValidation.optionalText(request.color(), "Color", 100),
                CatalogValidation.requiredText(request.unitType(), "Unit type", 50),
                CatalogValidation.money(request.unitSellingPrice(), "Unit selling price"),
                CatalogValidation.money(request.unitCost(), "Unit cost"),
                CatalogValidation.money(request.installationFee(), "Installation fee"),
                optionalPositiveQuantity(request.minimumQuantity(), "Minimum quantity"),
                Boolean.TRUE.equals(request.active()),
                request.effectiveDate());
        HairConfiguration saved = configurationRepository.saveAndFlush(configuration);
        return HairConfigurationResponse.from(saved, true, stockFor(saved));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SERVICES_AND_PRICES')")
    public HairConfigurationResponse setActive(Long configurationId, boolean active) {
        HairConfiguration configuration = getEntityById(configurationId);
        configuration.setActive(active);
        HairConfiguration saved = configurationRepository.saveAndFlush(configuration);
        return HairConfigurationResponse.from(saved, true, stockFor(saved));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('SERVICES_AND_PRICES','ADD_ORDERS')")
    public HairConfigurationResponse getById(Long configurationId) {
        HairConfiguration configuration = getEntityById(configurationId);
        return HairConfigurationResponse.from(configuration, canViewCosts(), stockFor(configuration));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('SERVICES_AND_PRICES','ADD_ORDERS')")
    public PageResponse<HairConfigurationResponse> search(
            Long hairItemId,
            Boolean active,
            LocalDate effectiveOn,
            String query,
            int page,
            int size) {
        String search = query == null || query.isBlank()
                ? null
                : "%" + query.strip().toLowerCase(Locale.ROOT) + "%";
        Page<HairConfiguration> configurations = configurationRepository.search(
                hairItemId,
                active,
                effectiveOn,
                search,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("effectiveDate"),
                                Sort.Order.asc("brand"),
                                Sort.Order.asc("id"))));
        Map<Long, com.omar.beautycenter.catalog.dto.HairStockResponse> stockByItem =
                hairStockService.findByHairItemIds(configurations.getContent().stream()
                        .map(configuration -> configuration.getHairItem().getId())
                        .distinct()
                        .toList());
        return PageResponse.from(configurations.map(configuration -> HairConfigurationResponse.from(
                configuration,
                canViewCosts(),
                stockByItem.get(configuration.getHairItem().getId()))));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('SERVICES_AND_PRICES')")
    public PageResponse<HairPriceOverrideAuditResponse> searchOverrides(
            Long configurationId,
            int page,
            int size) {
        Page<HairPriceOverrideAuditResponse> result = overrideAuditRepository.search(
                        configurationId,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))))
                .map(HairPriceOverrideAuditResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public HairConfiguration getEntityById(Long configurationId) {
        return configurationRepository.findById(configurationId)
                .orElseThrow(() -> new NotFoundException("Hair configuration not found."));
    }

    @Transactional(readOnly = true)
    public HairConfiguration getActiveEffectiveEntity(Long configurationId, LocalDate calculationDate) {
        HairConfiguration configuration = getEntityById(configurationId);
        if (!configuration.isActive()) {
            throw new BusinessRuleException("Inactive hair configurations cannot be calculated.");
        }
        if (configuration.getEffectiveDate().isAfter(calculationDate)) {
            throw new BusinessRuleException("A hair configuration cannot be used before its effective date.");
        }
        getActiveHairItem(configuration.getHairItem().getId());
        return configuration;
    }

    private CatalogItem getActiveHairItem(Long itemId) {
        CatalogItem item = itemService.getActiveEntityById(itemId);
        if (item.getItemType() != CatalogItemType.HAIR_PRODUCT) {
            throw new BusinessRuleException("Hair configurations must reference an active HAIR_PRODUCT catalog item.");
        }
        return item;
    }

    private com.omar.beautycenter.catalog.dto.HairStockResponse stockFor(HairConfiguration configuration) {
        return hairStockService.findByHairItemIds(List.of(configuration.getHairItem().getId()))
                .get(configuration.getHairItem().getId());
    }

    static BigDecimal positiveQuantity(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessRuleException(field + " must be greater than zero.");
        }
        try {
            BigDecimal normalized = value.setScale(3, RoundingMode.UNNECESSARY);
            if (normalized.stripTrailingZeros().scale() > 0) {
                throw new BusinessRuleException(field + " must be a whole number of grams.");
            }
            return normalized;
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException(field + " must have at most 3 decimal digits.");
        }
    }

    private static BigDecimal optionalPositiveQuantity(BigDecimal value, String field) {
        return value == null ? null : positiveQuantity(value, field);
    }

    private static boolean canViewCosts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER")
                        || authority.getAuthority().equals("PERMISSION_SERVICES_AND_PRICES"));
    }
}
