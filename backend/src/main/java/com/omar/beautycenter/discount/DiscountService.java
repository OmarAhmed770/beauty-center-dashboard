package com.omar.beautycenter.discount;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.catalog.CatalogCategory;
import com.omar.beautycenter.catalog.CatalogCategoryService;
import com.omar.beautycenter.catalog.CatalogItem;
import com.omar.beautycenter.catalog.CatalogItemService;
import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import com.omar.beautycenter.discount.dto.CreateDiscountRequest;
import com.omar.beautycenter.discount.dto.DiscountResponse;
import com.omar.beautycenter.discount.dto.UpdateDiscountRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscountService {

    private static final BigDecimal MAXIMUM_PERCENTAGE = new BigDecimal("100.00");

    private final DiscountRepository discountRepository;
    private final CatalogItemService itemService;
    private final CatalogCategoryService categoryService;
    private final AuditLogService auditLogService;

    public DiscountService(
            DiscountRepository discountRepository,
            CatalogItemService itemService,
            CatalogCategoryService categoryService,
            AuditLogService auditLogService) {
        this.discountRepository = discountRepository;
        this.itemService = itemService;
        this.categoryService = categoryService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SETTINGS')")
    public DiscountResponse create(CreateDiscountRequest request) {
        Scope scope = resolveScope(request.catalogItemId(), request.categoryId());
        Discount discount = new Discount(
                requiredText(request.name(), "Discount name", 150),
                requiredType(request.type()),
                validValue(request.type(), request.value()),
                request.startDate(),
                request.endDate(),
                Boolean.TRUE.equals(request.active()),
                scope.catalogItem(),
                scope.category());
        validateDates(discount.getStartDate(), discount.getEndDate());
        discountRepository.saveAndFlush(discount);
        auditLogService.record("DISCOUNT_CREATED", "DISCOUNT", discount.getId(), null,
                Map.of("name", discount.getName(), "type", discount.getType(), "value", discount.getValue()));
        return DiscountResponse.from(discount);
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SETTINGS')")
    public DiscountResponse update(Long discountId, UpdateDiscountRequest request) {
        Discount discount = getEntityById(discountId);
        Scope scope = resolveScope(request.catalogItemId(), request.categoryId());
        validateDates(request.startDate(), request.endDate());
        discount.update(
                requiredText(request.name(), "Discount name", 150),
                requiredType(request.type()),
                validValue(request.type(), request.value()),
                request.startDate(),
                request.endDate(),
                scope.catalogItem(),
                scope.category());
        discountRepository.saveAndFlush(discount);
        auditLogService.record("DISCOUNT_UPDATED", "DISCOUNT", discount.getId(), null,
                Map.of("name", discount.getName(), "type", discount.getType(), "value", discount.getValue()));
        return DiscountResponse.from(discount);
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SETTINGS')")
    public DiscountResponse setActive(Long discountId, boolean active) {
        Discount discount = getEntityById(discountId);
        discount.setActive(active);
        discountRepository.saveAndFlush(discount);
        auditLogService.record("DISCOUNT_ACTIVE_CHANGED", "DISCOUNT", discount.getId(), null,
                Map.of("active", active));
        return DiscountResponse.from(discount);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('SETTINGS','ADD_ORDERS')")
    public DiscountResponse getById(Long discountId) {
        return DiscountResponse.from(getEntityById(discountId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('SETTINGS','ADD_ORDERS')")
    public PageResponse<DiscountResponse> search(
            Boolean active,
            DiscountType type,
            Long catalogItemId,
            Long categoryId,
            LocalDate applicableDate,
            String query,
            int page,
            int size) {
        String search = query == null || query.isBlank()
                ? null
                : "%" + query.strip().toLowerCase(Locale.ROOT) + "%";
        Page<DiscountResponse> result = discountRepository.search(
                        active,
                        type,
                        catalogItemId,
                        categoryId,
                        applicableDate,
                        search,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Order.desc("startDate"), Sort.Order.asc("name"))))
                .map(DiscountResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public Discount getEntityById(Long discountId) {
        return discountRepository.findById(discountId)
                .orElseThrow(() -> new NotFoundException("Discount not found."));
    }

    private Scope resolveScope(Long catalogItemId, Long categoryId) {
        if (catalogItemId != null && categoryId != null) {
            throw new BusinessRuleException("A discount may target an item or a category, but not both.");
        }
        CatalogItem item = catalogItemId == null ? null : itemService.getActiveEntityById(catalogItemId);
        CatalogCategory category = categoryId == null ? null : categoryService.getActiveEntityById(categoryId);
        return new Scope(item, category);
    }

    private static void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessRuleException("Start date and end date are required.");
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessRuleException("Start date must be on or before end date.");
        }
    }

    private static DiscountType requiredType(DiscountType type) {
        if (type == null) {
            throw new BusinessRuleException("Discount type is required.");
        }
        return type;
    }

    private static BigDecimal validValue(DiscountType type, BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new BusinessRuleException("Discount value must be zero or greater.");
        }
        BigDecimal normalized;
        try {
            normalized = value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException("Discount value must have at most 2 decimal digits.");
        }
        if (type == DiscountType.PERCENTAGE && normalized.compareTo(MAXIMUM_PERCENTAGE) > 0) {
            throw new BusinessRuleException("Percentage discounts must be between 0 and 100.");
        }
        return normalized;
    }

    private static String requiredText(String value, String field, int maximumLength) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new BusinessRuleException(
                    field + " must contain 1 to " + maximumLength + " characters.");
        }
        return normalized;
    }

    private record Scope(CatalogItem catalogItem, CatalogCategory category) {
    }
}
