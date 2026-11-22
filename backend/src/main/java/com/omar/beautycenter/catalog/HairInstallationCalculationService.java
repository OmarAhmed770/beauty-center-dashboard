package com.omar.beautycenter.catalog;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.catalog.dto.HairCalculationPreviewRequest;
import com.omar.beautycenter.catalog.dto.HairCalculationPreviewResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.discount.Discount;
import com.omar.beautycenter.discount.DiscountCalculation;
import com.omar.beautycenter.discount.DiscountCalculationService;
import com.omar.beautycenter.discount.DiscountService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HairInstallationCalculationService {

    private final HairConfigurationService configurationService;
    private final HairPriceOverrideAuditRepository overrideAuditRepository;
    private final DiscountService discountService;
    private final DiscountCalculationService discountCalculationService;
    private final AuditorAware<String> auditorAware;
    private final Clock clock;
    private final AuditLogService auditLogService;

    public HairInstallationCalculationService(
            HairConfigurationService configurationService,
            HairPriceOverrideAuditRepository overrideAuditRepository,
            DiscountService discountService,
            DiscountCalculationService discountCalculationService,
            AuditorAware<String> auditorAware,
            Clock clock,
            AuditLogService auditLogService) {
        this.configurationService = configurationService;
        this.overrideAuditRepository = overrideAuditRepository;
        this.discountService = discountService;
        this.discountCalculationService = discountCalculationService;
        this.auditorAware = auditorAware;
        this.clock = clock;
        this.auditLogService = auditLogService;
    }

    @Transactional
    @PreAuthorize("@permissionAccess.hasAny('SERVICES_AND_PRICES','ADD_ORDERS')")
    public HairCalculationPreviewResponse preview(HairCalculationPreviewRequest request) {
        LocalDate calculationDate = LocalDate.now(clock);
        HairConfiguration configuration = configurationService.getActiveEffectiveEntity(
                request.configurationId(), calculationDate);
        BigDecimal quantity = HairConfigurationService.positiveQuantity(request.quantity(), "Quantity");
        if (configuration.getMinimumQuantity() != null
                && quantity.compareTo(configuration.getMinimumQuantity()) < 0) {
            throw new BusinessRuleException("Quantity is below the configuration minimum.");
        }

        BigDecimal baseProductAmount = multiplyMoney(configuration.getUnitSellingPrice(), quantity);
        BigDecimal installationFee = configuration.getInstallationFee().setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal subtotal = baseProductAmount.add(installationFee).setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal costEstimate = multiplyMoney(configuration.getUnitCost(), quantity);
        Discount discount = request.discountId() == null
                ? null
                : discountService.getEntityById(request.discountId());
        DiscountCalculation discounted = discountCalculationService.calculate(
                subtotal,
                discount,
                calculationDate,
                configuration.getHairItem());

        BigDecimal finalAmount = discounted.finalAmount();
        boolean overridden = request.overrideAmount() != null;
        if (overridden) {
            assertOverrideAuthorized();
            BigDecimal overrideAmount = CatalogValidation.money(request.overrideAmount(), "Override amount");
            String overrideReason = CatalogValidation.requiredText(
                    request.overrideReason(), "Override reason", 500);
            if (overrideAmount.compareTo(discounted.finalAmount()) == 0) {
                throw new BusinessRuleException("Override amount must differ from the calculated final amount.");
            }
            String actor = auditorAware.getCurrentAuditor().orElse("SYSTEM");
            overrideAuditRepository.save(new HairPriceOverrideAudit(
                    configuration,
                    quantity,
                    discounted.finalAmount(),
                    overrideAmount,
                    overrideReason,
                    actor,
                    LocalDateTime.now(clock)));
            auditLogService.record("PRICE_OVERRIDE", "HAIR_CONFIGURATION", configuration.getId(),
                    calculationDate, Map.of("originalAmount", discounted.finalAmount(),
                            "overrideAmount", overrideAmount, "reason", overrideReason,
                            "quantity", quantity));
            finalAmount = overrideAmount;
        } else if (request.overrideReason() != null && !request.overrideReason().isBlank()) {
            throw new BusinessRuleException("Override amount is required when an override reason is supplied.");
        }

        return new HairCalculationPreviewResponse(
                configuration.getId(),
                configuration.getHairItem().getId(),
                configuration.getHairItem().getCode(),
                quantity,
                calculationDate,
                baseProductAmount,
                installationFee,
                subtotal,
                discount == null ? null : discount.getId(),
                discount == null ? null : discount.getType(),
                discount == null ? null : discount.getValue(),
                discounted.discountAmount(),
                discounted.finalAmount(),
                overridden,
                finalAmount,
                canViewCosts() ? costEstimate : null);
    }

    private static BigDecimal multiplyMoney(BigDecimal unitValue, BigDecimal quantity) {
        return unitValue.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }

    private static void assertOverrideAuthorized() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean authorized = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER")
                        || authority.getAuthority().equals("PERMISSION_SERVICES_AND_PRICES"));
        if (!authorized) {
            throw new AccessDeniedException("Only owners or managers may override calculated prices.");
        }
    }

    private static boolean canViewCosts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER")
                        || authority.getAuthority().equals("PERMISSION_SERVICES_AND_PRICES"));
    }
}
