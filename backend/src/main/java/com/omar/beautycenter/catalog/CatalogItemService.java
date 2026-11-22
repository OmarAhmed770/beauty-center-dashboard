package com.omar.beautycenter.catalog;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.catalog.dto.CatalogItemResponse;
import com.omar.beautycenter.catalog.dto.CreateCatalogItemRequest;
import com.omar.beautycenter.catalog.dto.PriceHistoryResponse;
import com.omar.beautycenter.catalog.dto.UpdateCatalogItemRequest;
import com.omar.beautycenter.catalog.dto.UpdateCatalogPriceRequest;
import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogItemService {

    private final CatalogItemRepository itemRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final CatalogCategoryService categoryService;
    private final AuditorAware<String> auditorAware;
    private final Clock clock;
    private final AuditLogService auditLogService;

    public CatalogItemService(
            CatalogItemRepository itemRepository,
            PriceHistoryRepository priceHistoryRepository,
            CatalogCategoryService categoryService,
            AuditorAware<String> auditorAware,
            Clock clock,
            AuditLogService auditLogService) {
        this.itemRepository = itemRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.categoryService = categoryService;
        this.auditorAware = auditorAware;
        this.clock = clock;
        this.auditLogService = auditLogService;
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SERVICES_AND_PRICES')")
    public CatalogItemResponse create(CreateCatalogItemRequest request) {
        String code = CatalogValidation.code(request.code());
        if (itemRepository.existsByCode(code)) {
            throw new BusinessRuleException("A catalog item with this code already exists.");
        }
        if (request.itemType() == null) {
            throw new BusinessRuleException("Item type is required.");
        }
        CatalogCategory category = categoryService.getActiveEntityById(request.categoryId());
        CatalogItem item = new CatalogItem(
                code,
                CatalogValidation.requiredText(request.nameArabic(), "Arabic name", 150),
                CatalogValidation.requiredText(request.nameEnglish(), "English name", 150),
                request.itemType(),
                category,
                CatalogValidation.money(request.currentSellingPrice(), "Selling price"),
                CatalogValidation.money(request.currentCost(), "Cost"),
                Boolean.TRUE.equals(request.active()),
                CatalogValidation.optionalText(request.description(), "Description", 2000));
        return CatalogItemResponse.from(itemRepository.saveAndFlush(item), true);
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SERVICES_AND_PRICES')")
    public CatalogItemResponse update(Long itemId, UpdateCatalogItemRequest request) {
        CatalogItem item = getEntityById(itemId);
        CatalogCategory category = categoryService.getActiveEntityById(request.categoryId());
        item.update(
                CatalogValidation.requiredText(request.nameArabic(), "Arabic name", 150),
                CatalogValidation.requiredText(request.nameEnglish(), "English name", 150),
                category,
                CatalogValidation.optionalText(request.description(), "Description", 2000));
        return CatalogItemResponse.from(itemRepository.saveAndFlush(item), true);
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SERVICES_AND_PRICES')")
    public CatalogItemResponse updatePrices(Long itemId, UpdateCatalogPriceRequest request) {
        CatalogItem item = getEntityById(itemId);
        String reason = CatalogValidation.requiredText(request.reason(), "Change reason", 500);
        LocalDateTime effectiveAt = LocalDateTime.now(clock);
        String actor = auditorAware.getCurrentAuditor().orElse("SYSTEM");
        boolean changed = false;

        if (request.sellingPrice() != null) {
            BigDecimal newSellingPrice = CatalogValidation.money(request.sellingPrice(), "Selling price");
            if (item.getCurrentSellingPrice().compareTo(newSellingPrice) != 0) {
                priceHistoryRepository.save(new PriceHistory(
                        item,
                        PriceValueType.SELLING_PRICE,
                        item.getCurrentSellingPrice(),
                        newSellingPrice,
                        effectiveAt,
                        reason,
                        actor));
                item.updateSellingPrice(newSellingPrice);
                changed = true;
            }
        }

        if (request.cost() != null) {
            BigDecimal newCost = CatalogValidation.money(request.cost(), "Cost");
            if (item.getCurrentCost().compareTo(newCost) != 0) {
                priceHistoryRepository.save(new PriceHistory(
                        item,
                        PriceValueType.COST,
                        item.getCurrentCost(),
                        newCost,
                        effectiveAt,
                        reason,
                        actor));
                item.updateCost(newCost);
                changed = true;
            }
        }

        if (!changed) {
            throw new BusinessRuleException("At least one price or cost value must change.");
        }
        itemRepository.saveAndFlush(item);
        auditLogService.record("CATALOG_PRICE_CHANGED", "CATALOG_ITEM", item.getId(), null,
                Map.of("sellingPrice", item.getCurrentSellingPrice(), "cost", item.getCurrentCost(),
                        "reason", reason));
        return CatalogItemResponse.from(item, true);
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('SERVICES_AND_PRICES')")
    public CatalogItemResponse setActive(Long itemId, boolean active) {
        CatalogItem item = getEntityById(itemId);
        item.setActive(active);
        return CatalogItemResponse.from(itemRepository.saveAndFlush(item), true);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('SERVICES_AND_PRICES','ADD_ORDERS')")
    public CatalogItemResponse getById(Long itemId) {
        return CatalogItemResponse.from(getEntityById(itemId), canViewCosts());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('SERVICES_AND_PRICES','ADD_ORDERS')")
    public PageResponse<CatalogItemResponse> search(
            Long categoryId,
            CatalogItemType itemType,
            Boolean active,
            String query,
            int page,
            int size) {
        String search = query == null || query.isBlank()
                ? null
                : "%" + query.strip().toLowerCase(Locale.ROOT) + "%";
        Page<CatalogItemResponse> result = itemRepository.search(
                        categoryId,
                        itemType,
                        active,
                        search,
                        PageRequest.of(page, size, Sort.by("nameEnglish").ascending()))
                .map(item -> CatalogItemResponse.from(item, canViewCosts()));
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('SERVICES_AND_PRICES')")
    public PageResponse<PriceHistoryResponse> searchPriceHistory(
            Long itemId,
            PriceValueType valueType,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime,
            int page,
            int size) {
        if (fromDateTime != null && toDateTime != null && fromDateTime.isAfter(toDateTime)) {
            throw new BusinessRuleException("fromDateTime must be on or before toDateTime.");
        }
        Page<PriceHistoryResponse> result = priceHistoryRepository.search(
                        itemId,
                        valueType,
                        fromDateTime,
                        toDateTime,
                        PageRequest.of(page, size, Sort.by(Direction.DESC, "effectiveAt", "id")))
                .map(PriceHistoryResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public CatalogItem getEntityById(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Catalog item not found."));
    }

    @Transactional(readOnly = true)
    public CatalogItem getActiveEntityById(Long itemId) {
        CatalogItem item = getEntityById(itemId);
        if (!item.isActive() || !item.getCategory().isActive()) {
            throw new BusinessRuleException("Inactive catalog items or categories cannot be used for new records.");
        }
        return item;
    }

    private static boolean canViewCosts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER")
                        || authority.getAuthority().equals("PERMISSION_SERVICES_AND_PRICES"));
    }
}
