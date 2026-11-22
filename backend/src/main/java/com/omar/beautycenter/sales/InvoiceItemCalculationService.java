package com.omar.beautycenter.sales;

import com.omar.beautycenter.catalog.CatalogItem;
import com.omar.beautycenter.catalog.CatalogItemType;
import com.omar.beautycenter.catalog.CatalogItemService;
import com.omar.beautycenter.catalog.HairConfiguration;
import com.omar.beautycenter.catalog.HairConfigurationService;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.discount.Discount;
import com.omar.beautycenter.discount.DiscountCalculation;
import com.omar.beautycenter.discount.DiscountCalculationService;
import com.omar.beautycenter.discount.DiscountService;
import com.omar.beautycenter.employee.Employee;
import com.omar.beautycenter.employee.EmployeeService;
import com.omar.beautycenter.sales.dto.UpsertInvoiceItemRequest;
import com.omar.beautycenter.sales.dto.QuickOrderLineRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class InvoiceItemCalculationService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal MANUAL_QUANTITY = new BigDecimal("1.000");
    private static final String MANUAL_ITEM_CODE_PREFIX = "MANUAL-";

    private final CatalogItemService catalogItemService;
    private final HairConfigurationService hairConfigurationService;
    private final DiscountService discountService;
    private final DiscountCalculationService discountCalculationService;
    private final EmployeeService employeeService;
    private final AuditorAware<String> auditorAware;
    private final Clock clock;

    public InvoiceItemCalculationService(
            CatalogItemService catalogItemService,
            HairConfigurationService hairConfigurationService,
            DiscountService discountService,
            DiscountCalculationService discountCalculationService,
            EmployeeService employeeService,
            AuditorAware<String> auditorAware,
            Clock clock) {
        this.catalogItemService = catalogItemService;
        this.hairConfigurationService = hairConfigurationService;
        this.discountService = discountService;
        this.discountCalculationService = discountCalculationService;
        this.employeeService = employeeService;
        this.auditorAware = auditorAware;
        this.clock = clock;
    }

    InvoiceItemSnapshot calculate(UpsertInvoiceItemRequest request, LocalDate businessDate) {
        CatalogItem item = catalogItemService.getActiveEntityById(request.catalogItemId());
        BigDecimal quantity = positiveQuantity(request.quantity());
        HairConfiguration hairConfiguration = null;
        BigDecimal unitSellingPrice = item.getCurrentSellingPrice();
        BigDecimal unitCost = item.getCurrentCost();
        BigDecimal installationFee = ZERO;

        if (request.hairConfigurationId() != null) {
            hairConfiguration = hairConfigurationService.getActiveEffectiveEntity(
                    request.hairConfigurationId(), businessDate);
            if (!hairConfiguration.getHairItem().getId().equals(item.getId())) {
                throw new BusinessRuleException(
                        "The hair configuration does not belong to the selected catalog item.");
            }
            if (hairConfiguration.getMinimumQuantity() != null
                    && quantity.compareTo(hairConfiguration.getMinimumQuantity()) < 0) {
                throw new BusinessRuleException("Quantity is below the hair configuration minimum.");
            }
            unitSellingPrice = hairConfiguration.getUnitSellingPrice();
            unitCost = hairConfiguration.getUnitCost();
            installationFee = hairConfiguration.getInstallationFee();
        }

        BigDecimal lineSubtotal = multiplyMoney(unitSellingPrice, quantity)
                .add(installationFee)
                .setScale(2, RoundingMode.UNNECESSARY);
        Discount discount = request.discountId() == null
                ? null
                : discountService.getEntityById(request.discountId());
        DiscountCalculation discountCalculation = discountCalculationService.calculate(
                lineSubtotal, discount, businessDate, item);
        Employee employee = request.employeeId() == null
                ? null
                : employeeService.getEntityById(request.employeeId());
        if (employee != null && !employee.isActive()) {
            throw new BusinessRuleException("Only active employees may be assigned to new invoice items.");
        }

        BigDecimal overrideOriginal = null;
        BigDecimal overrideAmount = null;
        String overrideReason = null;
        String overriddenBy = null;
        LocalDateTime overriddenAt = null;
        BigDecimal finalAmount = discountCalculation.finalAmount();
        if (request.priceOverrideAmount() != null) {
            assertOverrideAuthorized();
            overrideAmount = money(request.priceOverrideAmount(), "Price override amount");
            overrideReason = requiredText(request.priceOverrideReason(), "Price override reason", 500);
            if (overrideAmount.compareTo(finalAmount) == 0) {
                throw new BusinessRuleException(
                        "Price override amount must differ from the calculated final amount.");
            }
            overrideOriginal = finalAmount;
            overriddenBy = auditorAware.getCurrentAuditor().orElse("SYSTEM");
            overriddenAt = LocalDateTime.now(clock);
            finalAmount = overrideAmount;
        } else if (request.priceOverrideReason() != null
                && !request.priceOverrideReason().isBlank()) {
            throw new BusinessRuleException(
                    "Price override amount is required when a reason is supplied.");
        }

        return new InvoiceItemSnapshot(
                item,
                item.getCode(),
                item.getNameArabic(),
                item.getNameEnglish(),
                item.getItemType(),
                hairConfiguration,
                quantity,
                money(unitSellingPrice, "Selling price"),
                money(unitCost, "Cost"),
                money(installationFee, "Installation fee"),
                discount,
                discountCalculation.discountAmount(),
                lineSubtotal,
                finalAmount,
                employee,
                overrideOriginal,
                overrideAmount,
                overrideReason,
                overriddenBy,
                overriddenAt);
    }

    /**
     * Adapts a cashier quick-order line to the established immutable invoice item calculation.
     * Prices are intentionally loaded only from the catalog/hair configuration.
     */
    InvoiceItemSnapshot calculateQuickOrder(QuickOrderLineRequest request, LocalDate businessDate) {
        CatalogItem item = catalogItemService.getActiveEntityById(request.catalogItemId());
        BigDecimal quantity = request.hairGrams() == null ? request.quantity() : request.hairGrams();

        if (item.getItemType() == CatalogItemType.HAIR_PRODUCT) {
            if (request.hairConfigurationId() == null || request.hairGrams() == null) {
                throw new BusinessRuleException(
                        "Hair products require an active hair configuration and grams.");
            }
        } else if (request.hairGrams() != null) {
            throw new BusinessRuleException("Hair grams may only be supplied for hair products.");
        }

        return calculate(new UpsertInvoiceItemRequest(
                request.catalogItemId(),
                request.hairConfigurationId(),
                quantity,
                null,
                request.employeeId(),
                null,
                null), businessDate);
    }

    InvoiceItemSnapshot calculateManual(
            String description,
            BigDecimal totalAmount,
            Long employeeId) {
        String normalizedDescription = requiredText(description, "Description", 150);
        BigDecimal amount = positiveMoney(totalAmount, "Total amount");
        Employee employee = employeeId == null ? null : employeeService.getEntityById(employeeId);
        if (employee != null && !employee.isActive()) {
            throw new BusinessRuleException("Only active employees may be assigned to new invoice items.");
        }
        return new InvoiceItemSnapshot(
                null,
                MANUAL_ITEM_CODE_PREFIX + UUID.randomUUID().toString().replace("-", "")
                        .substring(0, 12).toUpperCase(Locale.ROOT),
                normalizedDescription,
                normalizedDescription,
                CatalogItemType.SERVICE,
                null,
                MANUAL_QUANTITY,
                amount,
                ZERO,
                ZERO,
                null,
                ZERO,
                amount,
                amount,
                employee,
                null,
                null,
                null,
                null,
                null);
    }

    void validateSnapshotForCompletion(InvoiceItem item, LocalDate businessDate) {
        if (item.isManualItem()) {
            return;
        }
        CatalogItem activeItem = catalogItemService.getActiveEntityById(item.getCatalogItem().getId());
        if (item.getHairConfiguration() != null) {
            hairConfigurationService.getActiveEffectiveEntity(
                    item.getHairConfiguration().getId(), businessDate);
        }
        if (item.getDiscount() != null) {
            DiscountCalculation current = discountCalculationService.calculate(
                    item.getLineSubtotal(), item.getDiscount(), businessDate, activeItem);
            if (current.discountAmount().compareTo(item.getDiscountAmount()) != 0) {
                throw new BusinessRuleException(
                        "Draft item discount data changed; update the item before completion.");
            }
        }
    }

    static BigDecimal positiveMoney(BigDecimal amount, String field) {
        BigDecimal normalized = money(amount, field);
        if (normalized.signum() <= 0) {
            throw new BusinessRuleException(field + " must be greater than zero.");
        }
        return normalized;
    }

    static BigDecimal money(BigDecimal amount, String field) {
        if (amount == null || amount.signum() < 0) {
            throw new BusinessRuleException(field + " must be zero or greater.");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException(field + " must have at most 2 decimal digits.");
        }
    }

    static String optionalText(String value, String field, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maximumLength) {
            throw new BusinessRuleException(field + " must not exceed " + maximumLength + " characters.");
        }
        return normalized;
    }

    static String requiredText(String value, String field, int maximumLength) {
        String normalized = optionalText(value, field, maximumLength);
        if (normalized == null) {
            throw new BusinessRuleException(field + " is required.");
        }
        return normalized;
    }

    private static BigDecimal positiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("Quantity must be greater than zero.");
        }
        try {
            BigDecimal normalized = quantity.setScale(3, RoundingMode.UNNECESSARY);
            if (normalized.stripTrailingZeros().scale() > 0) {
                throw new BusinessRuleException("Quantity must be a whole number.");
            }
            return normalized;
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException("Quantity must have at most 3 decimal digits.");
        }
    }

    private static BigDecimal multiplyMoney(BigDecimal unitValue, BigDecimal quantity) {
        return unitValue.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }

    private static void assertOverrideAuthorized() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authorized = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER")
                        || authority.getAuthority().equals("PERMISSION_INVOICES_AND_RETURNS"));
        if (!authorized) {
            throw new AccessDeniedException(
                    "Only owners or managers may override invoice item prices.");
        }
    }
}
