package com.omar.beautycenter.closing;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.closing.dto.CloseDailyRequest;
import com.omar.beautycenter.closing.dto.DailyClosingPreviewResponse;
import com.omar.beautycenter.closing.dto.DailyClosingResponse;
import com.omar.beautycenter.closing.dto.ReopenDailyRequest;
import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import com.omar.beautycenter.report.FinancialQueryService;
import com.omar.beautycenter.report.dto.FinancialSummaryResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyClosingService {

    private final DailyClosingRepository closingRepository;
    private final DailyClosingPaymentTotalRepository paymentTotalRepository;
    private final DailyClosingExpenseTotalRepository expenseTotalRepository;
    private final FinancialQueryService financialQueryService;
    private final AuditLogService auditLogService;
    private final AuditorAware<String> auditorAware;
    private final Clock clock;
    private final BusinessDateTransactionLock transactionLock;

    public DailyClosingService(
            DailyClosingRepository closingRepository,
            DailyClosingPaymentTotalRepository paymentTotalRepository,
            DailyClosingExpenseTotalRepository expenseTotalRepository,
            FinancialQueryService financialQueryService,
            AuditLogService auditLogService,
            AuditorAware<String> auditorAware,
            Clock clock,
            BusinessDateTransactionLock transactionLock) {
        this.closingRepository = closingRepository;
        this.paymentTotalRepository = paymentTotalRepository;
        this.expenseTotalRepository = expenseTotalRepository;
        this.financialQueryService = financialQueryService;
        this.auditLogService = auditLogService;
        this.auditorAware = auditorAware;
        this.clock = clock;
        this.transactionLock = transactionLock;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('DAILY_CLOSING')")
    public DailyClosingPreviewResponse preview(LocalDate businessDate) {
        requireDate(businessDate);
        DailyClosingStatus status = closingRepository.findTopByBusinessDateOrderByClosingVersionDesc(businessDate)
                .map(DailyClosing::getStatus)
                .orElse(DailyClosingStatus.OPEN);
        return new DailyClosingPreviewResponse(
                businessDate, status, financialQueryService.summary(businessDate, businessDate));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('DAILY_CLOSING')")
    public DailyClosingResponse close(LocalDate businessDate, CloseDailyRequest request) {
        requireDate(businessDate);
        transactionLock.acquire(businessDate);
        DailyClosing latestClosing = closingRepository.findLatestByBusinessDateForUpdate(businessDate)
                .orElse(null);
        if (latestClosing != null && latestClosing.getStatus() == DailyClosingStatus.CLOSED) {
            throw new BusinessRuleException("The business date is already closed.");
        }

        FinancialSummaryResponse financials = financialQueryService.summary(businessDate, businessDate);
        BigDecimal actualCash = money(request.actualCash());
        String actor = auditorAware.getCurrentAuditor().orElse("SYSTEM");
        int nextClosingVersion = latestClosing == null ? 1 : latestClosing.getClosingVersion() + 1;
        DailyClosing closing = new DailyClosing(businessDate, nextClosingVersion);
        closing.close(financials, actualCash, optionalText(request.notes(), 2000), actor,
                LocalDateTime.now(clock));
        closing = closingRepository.saveAndFlush(closing);
        DailyClosing storedClosing = closing;

        List<DailyClosingPaymentTotal> payments = paymentTotalRepository.saveAllAndFlush(
                financials.paymentTotalsByMethod().stream()
                        .map(total -> new DailyClosingPaymentTotal(storedClosing, total)).toList());
        List<DailyClosingExpenseTotal> expenses = expenseTotalRepository.saveAllAndFlush(
                financials.expenseTotalsByCategory().stream()
                        .map(total -> new DailyClosingExpenseTotal(storedClosing, total)).toList());

        auditLogService.record("DAILY_CLOSING_COMPLETED", "DAILY_CLOSING", closing.getId(), businessDate,
                Map.of("expectedCash", closing.getExpectedCash(), "actualCash", closing.getActualCash(),
                        "cashDifference", closing.getCashDifference(), "netProfit", closing.getNetProfit(),
                        "closingVersion", closing.getClosingVersion()));
        return DailyClosingResponse.from(closing, payments, expenses);
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public DailyClosingResponse reopen(LocalDate businessDate, ReopenDailyRequest request) {
        requireDate(businessDate);
        transactionLock.acquire(businessDate);
        DailyClosing closing = closingRepository.findLatestByBusinessDateForUpdate(businessDate)
                .orElseThrow(() -> new NotFoundException("Daily closing not found."));
        String reason = requiredText(request.reason(), 500);
        String actor = auditorAware.getCurrentAuditor().orElse("SYSTEM");
        closing.reopen(reason, actor, LocalDateTime.now(clock));
        closingRepository.saveAndFlush(closing);
        auditLogService.record("DAILY_CLOSING_REOPENED", "DAILY_CLOSING", closing.getId(), businessDate,
                Map.of("reason", reason, "reopenCount", closing.getReopenCount(),
                        "closingVersion", closing.getClosingVersion()));
        return response(closing);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('DAILY_CLOSING')")
    public DailyClosingResponse getStored(LocalDate businessDate) {
        requireDate(businessDate);
        return response(closingRepository.findTopByBusinessDateOrderByClosingVersionDesc(businessDate)
                .orElseThrow(() -> new NotFoundException("Daily closing not found.")));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('DAILY_CLOSING')")
    public List<DailyClosingResponse> versions(LocalDate businessDate) {
        requireDate(businessDate);
        return closingRepository.findByBusinessDateOrderByClosingVersionDesc(businessDate).stream()
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('DAILY_CLOSING')")
    public PageResponse<DailyClosingResponse> search(
            DailyClosingStatus status, LocalDate fromDate, LocalDate toDate, int page, int size) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("fromDate must be on or before toDate.");
        }
        return PageResponse.from(closingRepository.search(status, fromDate, toDate,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("businessDate"),
                        Sort.Order.desc("closingVersion"))))
                .map(this::response));
    }

    private DailyClosingResponse response(DailyClosing closing) {
        return DailyClosingResponse.from(
                closing,
                paymentTotalRepository.findByClosingIdOrderByCode(closing.getId()),
                expenseTotalRepository.findByClosingIdOrderByAmountDesc(closing.getId()));
    }

    private static void requireDate(LocalDate businessDate) {
        if (businessDate == null) {
            throw new BusinessRuleException("Business date is required.");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new BusinessRuleException("Actual cash must be zero or greater.");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException("Actual cash must have at most 2 decimal digits.");
        }
    }

    private static String requiredText(String value, int maximumLength) {
        String result = optionalText(value, maximumLength);
        if (result == null) {
            throw new BusinessRuleException("Reopening reason is required.");
        }
        return result;
    }

    private static String optionalText(String value, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String result = value.strip();
        if (result.length() > maximumLength) {
            throw new BusinessRuleException("Text exceeds the supported length.");
        }
        return result;
    }
}
