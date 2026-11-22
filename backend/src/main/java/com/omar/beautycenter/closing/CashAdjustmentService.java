package com.omar.beautycenter.closing;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.closing.dto.CancelCashAdjustmentRequest;
import com.omar.beautycenter.closing.dto.CashAdjustmentResponse;
import com.omar.beautycenter.closing.dto.CreateCashAdjustmentRequest;
import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CashAdjustmentService {

    private final CashAdjustmentRepository repository;
    private final BusinessDateWriteGuard dateWriteGuard;
    private final AuditLogService auditLogService;
    private final AuditorAware<String> auditorAware;
    private final Clock clock;

    public CashAdjustmentService(
            CashAdjustmentRepository repository,
            BusinessDateWriteGuard dateWriteGuard,
            AuditLogService auditLogService,
            AuditorAware<String> auditorAware,
            Clock clock) {
        this.repository = repository;
        this.dateWriteGuard = dateWriteGuard;
        this.auditLogService = auditLogService;
        this.auditorAware = auditorAware;
        this.clock = clock;
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('DAILY_CLOSING')")
    public CashAdjustmentResponse create(CreateCashAdjustmentRequest request) {
        dateWriteGuard.assertWritable(request.businessDate());
        CashAdjustment adjustment = repository.saveAndFlush(new CashAdjustment(
                request.businessDate(), request.type(), positiveMoney(request.amount()), required(request.reason())));
        auditLogService.record("CASH_ADJUSTMENT_CREATED", "CASH_ADJUSTMENT", adjustment.getId(),
                adjustment.getBusinessDate(), Map.of("type", adjustment.getType(), "amount", adjustment.getAmount()));
        return CashAdjustmentResponse.from(adjustment);
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('DAILY_CLOSING')")
    public CashAdjustmentResponse cancel(Long id, CancelCashAdjustmentRequest request) {
        CashAdjustment adjustment = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cash adjustment not found."));
        if (adjustment.getStatus() == CashAdjustmentStatus.CANCELLED) {
            throw new BusinessRuleException("The cash adjustment is already cancelled.");
        }
        dateWriteGuard.assertWritable(adjustment.getBusinessDate());
        String reason = required(request.reason());
        adjustment.cancel(auditorAware.getCurrentAuditor().orElse("SYSTEM"), LocalDateTime.now(clock), reason);
        repository.saveAndFlush(adjustment);
        auditLogService.record("CASH_ADJUSTMENT_CANCELLED", "CASH_ADJUSTMENT", adjustment.getId(),
                adjustment.getBusinessDate(), Map.of("reason", reason));
        return CashAdjustmentResponse.from(adjustment);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('DAILY_CLOSING')")
    public PageResponse<CashAdjustmentResponse> search(
            CashAdjustmentType type, CashAdjustmentStatus status, LocalDate fromDate, LocalDate toDate,
            int page, int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("fromDate must be on or before toDate.");
        }
        return PageResponse.from(repository.search(type, status, fromDate, toDate,
                PageRequest.of(page, size,
                        Sort.by(Sort.Order.desc("businessDate"), Sort.Order.desc("id"))))
                .map(CashAdjustmentResponse::from));
    }

    private static BigDecimal positiveMoney(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessRuleException("Amount must be greater than zero.");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException("Amount must have at most 2 decimal digits.");
        }
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("Reason is required.");
        }
        return value.strip();
    }
}
