package com.omar.beautycenter.catalog;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.catalog.dto.AdjustHairStockRequest;
import com.omar.beautycenter.catalog.dto.HairStockMovementResponse;
import com.omar.beautycenter.catalog.dto.HairStockResponse;
import com.omar.beautycenter.catalog.dto.InitializeHairStockRequest;
import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import com.omar.beautycenter.sales.Invoice;
import com.omar.beautycenter.sales.InvoiceItem;
import com.omar.beautycenter.sales.InvoiceRefund;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tracks grams for actual HAIR_PRODUCT catalog items. A missing stock row deliberately means that
 * the owner has not supplied a trusted opening balance yet; it never means zero or unlimited stock.
 */
@Service
public class HairStockService {

    private final CatalogItemService catalogItemService;
    private final HairProductStockRepository stockRepository;
    private final HairStockMovementRepository movementRepository;
    private final AuditLogService auditLogService;
    private final AuditorAware<String> auditorAware;
    private final Clock clock;

    public HairStockService(
            CatalogItemService catalogItemService,
            HairProductStockRepository stockRepository,
            HairStockMovementRepository movementRepository,
            AuditLogService auditLogService,
            AuditorAware<String> auditorAware,
            Clock clock) {
        this.catalogItemService = catalogItemService;
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.auditLogService = auditLogService;
        this.auditorAware = auditorAware;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('SERVICES_AND_PRICES','ADD_ORDERS')")
    public HairStockResponse getStock(Long hairItemId) {
        CatalogItem item = getHairProduct(hairItemId);
        return stockRepository.findByHairItemId(item.getId())
                .map(HairStockResponse::from)
                .orElseGet(() -> HairStockResponse.unconfigured(item));
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public HairStockResponse initialize(Long hairItemId, InitializeHairStockRequest request) {
        CatalogItem item = getHairProduct(hairItemId);
        BigDecimal quantity = nonNegativeQuantity(request.quantityGrams(), "Initial stock quantity");
        String reason = requiredReason(request.reason());
        if (stockRepository.findByHairItemIdForUpdate(item.getId()).isPresent()) {
            throw new BusinessRuleException("Hair stock has already been initialized. Use an adjustment instead.");
        }
        HairProductStock stock = stockRepository.saveAndFlush(new HairProductStock(item, quantity));
        recordMovement(stock, null, null, HairStockMovementType.INITIAL, "INITIAL:" + item.getId(),
                BigDecimal.ZERO.setScale(3), quantity, reason);
        audit("HAIR_STOCK_INITIALIZED", item, null, quantity, quantity, reason);
        return HairStockResponse.from(stock);
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public HairStockResponse adjust(Long hairItemId, AdjustHairStockRequest request) {
        CatalogItem item = getHairProduct(hairItemId);
        BigDecimal adjustment = signedQuantity(request.adjustmentGrams(), "Stock adjustment");
        String reason = requiredReason(request.reason());
        HairProductStock stock = getConfiguredStockForUpdate(item.getId());
        BigDecimal previous = stock.getAvailableGrams();
        BigDecimal next = previous.add(adjustment).setScale(3, RoundingMode.UNNECESSARY);
        if (next.signum() < 0) {
            throw new BusinessRuleException("Hair stock cannot become negative.");
        }
        stock.adjust(adjustment);
        stockRepository.saveAndFlush(stock);
        recordMovement(stock, null, null, HairStockMovementType.MANUAL_ADJUSTMENT,
                "MANUAL:" + UUID.randomUUID(), previous, adjustment, reason);
        audit("HAIR_STOCK_ADJUSTED", item, previous, adjustment, next, reason);
        return HairStockResponse.from(stock);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('OWNER')")
    public PageResponse<HairStockMovementResponse> movements(Long hairItemId, int page, int size) {
        getHairProduct(hairItemId);
        return PageResponse.from(movementRepository.findAllByHairItemId(
                hairItemId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))))
                .map(HairStockMovementResponse::from));
    }

    @Transactional(readOnly = true)
    public Map<Long, HairStockResponse> findByHairItemIds(List<Long> hairItemIds) {
        if (hairItemIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, HairStockResponse> result = new HashMap<>();
        for (HairProductStock stock : stockRepository.findAllByHairItemIdIn(hairItemIds)) {
            result.put(stock.getHairItem().getId(), HairStockResponse.from(stock));
        }
        return result;
    }

    /** Called only by invoice completion while its encompassing transaction is still open. */
    @Transactional
    public void deductCompletedInvoiceHair(Invoice invoice, List<InvoiceItem> items) {
        List<InvoiceItem> hairItems = items.stream()
                .filter(item -> item.getHairConfiguration() != null)
                .toList();
        if (hairItems.isEmpty()) {
            return;
        }
        Map<Long, HairProductStock> lockedStocks = lockConfiguredStocks(hairItems);
        for (InvoiceItem item : hairItems) {
            String eventKey = "SALE:" + item.getId();
            if (movementRepository.existsByEventKey(eventKey)) {
                continue;
            }
            HairProductStock stock = lockedStocks.get(item.getCatalogItem().getId());
            BigDecimal soldGrams = quantity(item.getQuantity(), "Hair quantity", false);
            BigDecimal previous = stock.getAvailableGrams();
            if (previous.compareTo(soldGrams) < 0) {
                throw new BusinessRuleException("Insufficient available hair stock for "
                        + item.getItemNameArabicSnapshot() + ".");
            }
            stock.adjust(soldGrams.negate());
            stockRepository.saveAndFlush(stock);
            recordMovement(stock, item, null, HairStockMovementType.SALE, eventKey,
                    previous, soldGrams.negate(), "Completed invoice " + invoice.getInvoiceNumber());
            audit("HAIR_STOCK_SOLD", stock.getHairItem(), previous, soldGrams.negate(),
                    stock.getAvailableGrams(), "Invoice " + invoice.getInvoiceNumber());
        }
    }

    /** Restores only hair that this stock module previously deducted for this invoice. */
    @Transactional
    public void restoreCancelledInvoiceHair(Invoice invoice, List<InvoiceItem> items, String reason) {
        List<InvoiceItem> soldHairItems = items.stream()
                .filter(item -> item.getHairConfiguration() != null)
                .filter(item -> movementRepository.existsByEventKey("SALE:" + item.getId()))
                .toList();
        if (soldHairItems.isEmpty()) {
            return;
        }
        Map<Long, HairProductStock> lockedStocks = lockConfiguredStocks(soldHairItems);
        for (InvoiceItem item : soldHairItems) {
            String eventKey = "CANCEL:" + item.getId();
            if (movementRepository.existsByEventKey(eventKey)) {
                continue;
            }
            HairProductStock stock = lockedStocks.get(item.getCatalogItem().getId());
            BigDecimal returnedGrams = quantity(item.getQuantity(), "Hair quantity", false);
            BigDecimal previous = stock.getAvailableGrams();
            stock.adjust(returnedGrams);
            stockRepository.saveAndFlush(stock);
            recordMovement(stock, item, null, HairStockMovementType.CANCELLATION_RESTORE, eventKey,
                    previous, returnedGrams, reason);
            audit("HAIR_STOCK_CANCELLATION_RESTORED", stock.getHairItem(), previous, returnedGrams,
                    stock.getAvailableGrams(), reason);
        }
    }

    /** A financial refund restores stock only when the caller explicitly identifies returned grams. */
    @Transactional
    public void restoreRefundedHair(InvoiceRefund refund, Map<InvoiceItem, BigDecimal> returnedGrams, String reason) {
        if (returnedGrams == null || returnedGrams.isEmpty()) {
            return;
        }
        List<InvoiceItem> items = returnedGrams.keySet().stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getHairConfiguration() != null)
                .toList();
        if (items.size() != returnedGrams.size()) {
            throw new BusinessRuleException("Only hair invoice items may have returned grams.");
        }
        Map<Long, HairProductStock> lockedStocks = lockConfiguredStocks(items);
        for (InvoiceItem item : items) {
            BigDecimal grams = quantity(returnedGrams.get(item), "Returned hair quantity", false);
            BigDecimal alreadyReturned = movementRepository.sumAdjustmentsByInvoiceItemAndType(
                    item.getId(), HairStockMovementType.REFUND_RESTORE);
            if (alreadyReturned == null) {
                alreadyReturned = BigDecimal.ZERO;
            }
            if (alreadyReturned.add(grams).compareTo(item.getQuantity()) > 0) {
                throw new BusinessRuleException("Returned hair quantity exceeds the sold quantity.");
            }
            HairProductStock stock = lockedStocks.get(item.getCatalogItem().getId());
            BigDecimal previous = stock.getAvailableGrams();
            stock.adjust(grams);
            stockRepository.saveAndFlush(stock);
            recordMovement(stock, item, refund, HairStockMovementType.REFUND_RESTORE,
                    "REFUND:" + refund.getId() + ":" + item.getId(), previous, grams, reason);
            audit("HAIR_STOCK_REFUND_RESTORED", stock.getHairItem(), previous, grams,
                    stock.getAvailableGrams(), reason);
        }
    }

    private Map<Long, HairProductStock> lockConfiguredStocks(List<InvoiceItem> items) {
        List<Long> itemIds = items.stream()
                .map(InvoiceItem::getCatalogItem)
                .filter(Objects::nonNull)
                .map(CatalogItem::getId)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        Map<Long, HairProductStock> result = new HashMap<>();
        for (Long itemId : itemIds) {
            result.put(itemId, getConfiguredStockForUpdate(itemId));
        }
        return result;
    }

    private HairProductStock getConfiguredStockForUpdate(Long hairItemId) {
        return stockRepository.findByHairItemIdForUpdate(hairItemId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Hair stock has not been initialized for the selected hair product."));
    }

    private CatalogItem getHairProduct(Long hairItemId) {
        CatalogItem item = catalogItemService.getEntityById(hairItemId);
        if (item.getItemType() != CatalogItemType.HAIR_PRODUCT) {
            throw new BusinessRuleException("Stock can only be managed for hair products.");
        }
        return item;
    }

    private void recordMovement(
            HairProductStock stock,
            InvoiceItem invoiceItem,
            InvoiceRefund refund,
            HairStockMovementType type,
            String eventKey,
            BigDecimal previous,
            BigDecimal adjustment,
            String reason) {
        movementRepository.saveAndFlush(new HairStockMovement(
                stock.getHairItem(), invoiceItem, refund, type, eventKey,
                previous, adjustment, stock.getAvailableGrams(), reason,
                LocalDateTime.now(clock), auditorAware.getCurrentAuditor().orElse("SYSTEM")));
    }

    private void audit(
            String action,
            CatalogItem item,
            BigDecimal previous,
            BigDecimal adjustment,
            BigDecimal next,
            String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("hairItemId", item.getId());
        details.put("previousGrams", previous);
        details.put("adjustmentGrams", adjustment);
        details.put("newGrams", next);
        details.put("reason", reason);
        auditLogService.record(action, "HAIR_PRODUCT_STOCK", item.getId(), null, details);
    }

    private static BigDecimal nonNegativeQuantity(BigDecimal value, String field) {
        BigDecimal quantity = quantity(value, field, true);
        if (quantity.signum() < 0) {
            throw new BusinessRuleException(field + " cannot be negative.");
        }
        return quantity;
    }

    private static BigDecimal signedQuantity(BigDecimal value, String field) {
        BigDecimal quantity = quantity(value, field, false);
        if (quantity.signum() == 0) {
            throw new BusinessRuleException(field + " must not be zero.");
        }
        return quantity;
    }

    private static BigDecimal quantity(BigDecimal value, String field, boolean allowZero) {
        if (value == null || (!allowZero && value.signum() <= 0)) {
            throw new BusinessRuleException(field + (allowZero ? " is required." : " must be greater than zero."));
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

    private static String requiredReason(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Stock reason is required.");
        }
        String normalized = value.strip();
        if (normalized.length() > 500) {
            throw new BusinessRuleException("Stock reason must not exceed 500 characters.");
        }
        return normalized;
    }
}
