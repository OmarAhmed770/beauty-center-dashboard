package com.omar.beautycenter.expense;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.closing.BusinessDateWriteGuard;
import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import com.omar.beautycenter.expense.dto.CancelExpenseRequest;
import com.omar.beautycenter.expense.dto.CreateExpenseRequest;
import com.omar.beautycenter.expense.dto.ExpenseCategoryTotalResponse;
import com.omar.beautycenter.expense.dto.ExpensePaymentMethodTotalResponse;
import com.omar.beautycenter.expense.dto.ExpenseResponse;
import com.omar.beautycenter.expense.dto.ExpenseSummaryResponse;
import com.omar.beautycenter.payment.PaymentMethod;
import com.omar.beautycenter.payment.PaymentMethodService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final ExpenseRepository expenseRepository;
    private final ExpenseCategoryService categoryService;
    private final PaymentMethodService paymentMethodService;
    private final BusinessDateWriteGuard businessDateWriteGuard;
    private final AuditorAware<String> auditorAware;
    private final Clock clock;
    private final AuditLogService auditLogService;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            ExpenseCategoryService categoryService,
            PaymentMethodService paymentMethodService,
            BusinessDateWriteGuard businessDateWriteGuard,
            AuditorAware<String> auditorAware,
            Clock clock,
            AuditLogService auditLogService) {
        this.expenseRepository = expenseRepository;
        this.categoryService = categoryService;
        this.paymentMethodService = paymentMethodService;
        this.businessDateWriteGuard = businessDateWriteGuard;
        this.auditorAware = auditorAware;
        this.clock = clock;
        this.auditLogService = auditLogService;
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('EXPENSES')")
    public ExpenseResponse create(CreateExpenseRequest request) {
        if (request.businessDate() == null) {
            throw new BusinessRuleException("Business date is required.");
        }
        businessDateWriteGuard.assertWritable(request.businessDate());

        ExpenseCategory category = categoryService.getActiveEntityById(request.categoryId());
        PaymentMethod paymentMethod =
                paymentMethodService.getActiveEntityById(request.paymentMethodId());
        String paymentReference = ExpenseCategoryService.optionalText(
                request.paymentReference(), "Payment reference", 200);

        Expense expense = new Expense(
                category,
                positiveMoney(request.amount()),
                request.businessDate(),
                paymentMethod,
                paymentReference,
                ExpenseCategoryService.requiredText(request.description(), "Description", 1000),
                ExpenseCategoryService.optionalText(request.notes(), "Notes", 2000),
                normalizeReceiptPath(request.receiptPath()));
        return ExpenseResponse.from(expenseRepository.saveAndFlush(expense));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('EXPENSES')")
    public ExpenseResponse cancel(Long expenseId, CancelExpenseRequest request) {
        Expense expense = getEntityById(expenseId);
        if (expense.getStatus() == ExpenseStatus.CANCELLED) {
            throw new BusinessRuleException("The expense is already cancelled.");
        }
        if (isReceptionistWithoutElevatedRole()
                && expense.getBusinessDate().isBefore(LocalDate.now(clock))) {
            throw new AccessDeniedException("Receptionists cannot cancel old expenses.");
        }

        businessDateWriteGuard.assertWritable(expense.getBusinessDate());
        String reason = ExpenseCategoryService.requiredText(
                request.reason(), "Cancellation reason", 500);
        String actor = auditorAware.getCurrentAuditor().orElse("SYSTEM");
        expense.cancel(LocalDateTime.now(clock), actor, reason);
        expenseRepository.saveAndFlush(expense);
        auditLogService.record("EXPENSE_CANCELLED", "EXPENSE", expense.getId(),
                expense.getBusinessDate(), Map.of("amount", expense.getAmount(),
                        "categoryId", expense.getCategory().getId(), "reason", reason));
        return ExpenseResponse.from(expense);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('EXPENSES')")
    public ExpenseResponse getById(Long expenseId) {
        return ExpenseResponse.from(getEntityById(expenseId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('EXPENSES')")
    public PageResponse<ExpenseResponse> search(
            Long categoryId,
            Long paymentMethodId,
            ExpenseStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String query,
            int page,
            int size) {
        validateDateRange(fromDate, toDate, false);
        String search = query == null || query.isBlank()
                ? null
                : "%" + query.strip().toLowerCase(Locale.ROOT) + "%";
        Page<ExpenseResponse> result = expenseRepository.search(
                        categoryId,
                        paymentMethodId,
                        status,
                        fromDate,
                        toDate,
                        search,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Order.desc("businessDate"), Sort.Order.desc("id"))))
                .map(ExpenseResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('EXPENSES')")
    public ExpenseSummaryResponse summary(LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate, true);
        BigDecimal total = money(expenseRepository.sumActiveAmount(fromDate, toDate));
        BigDecimal cash = money(expenseRepository.sumActiveCashAmount(fromDate, toDate));
        return new ExpenseSummaryResponse(
                fromDate,
                toDate,
                expenseRepository.countByStatusAndDateRange(
                        ExpenseStatus.ACTIVE, fromDate, toDate),
                expenseRepository.countByStatusAndDateRange(
                        ExpenseStatus.CANCELLED, fromDate, toDate),
                total,
                cash,
                total.subtract(cash).setScale(2, RoundingMode.UNNECESSARY));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('EXPENSES')")
    public List<ExpenseCategoryTotalResponse> totalsByCategory(
            LocalDate fromDate,
            LocalDate toDate) {
        validateDateRange(fromDate, toDate, true);
        return expenseRepository.groupActiveByCategory(fromDate, toDate).stream()
                .map(total -> new ExpenseCategoryTotalResponse(
                        total.getCategoryId(),
                        total.getNameArabic(),
                        total.getNameEnglish(),
                        money(total.getTotalAmount()),
                        total.getExpenseCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('EXPENSES')")
    public List<ExpensePaymentMethodTotalResponse> totalsByPaymentMethod(
            LocalDate fromDate,
            LocalDate toDate) {
        validateDateRange(fromDate, toDate, true);
        return expenseRepository.groupActiveByPaymentMethod(fromDate, toDate).stream()
                .map(total -> new ExpensePaymentMethodTotalResponse(
                        total.getPaymentMethodId(),
                        total.getCode(),
                        total.getDisplayNameArabic(),
                        total.getDisplayNameEnglish(),
                        total.getType(),
                        money(total.getTotalAmount()),
                        total.getExpenseCount()))
                .toList();
    }

    private Expense getEntityById(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NotFoundException("Expense not found."));
    }

    private static BigDecimal positiveMoney(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Amount must be greater than zero.");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException("Amount must have at most 2 decimal digits.");
        }
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String normalizeReceiptPath(String receiptPath) {
        String normalized = ExpenseCategoryService.optionalText(
                receiptPath, "Receipt path", 500);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.replace('\\', '/');
        if (normalized.startsWith("/")
                || normalized.matches("^[A-Za-z]:.*")
                || normalized.contains("://")
                || normalized.equals("..")
                || normalized.startsWith("../")
                || normalized.contains("/../")) {
            throw new BusinessRuleException(
                    "Receipt path must be a managed relative path without parent traversal.");
        }
        return normalized;
    }

    private static void validateDateRange(
            LocalDate fromDate,
            LocalDate toDate,
            boolean required) {
        if (required && (fromDate == null || toDate == null)) {
            throw new BusinessRuleException("Both fromDate and toDate are required.");
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BusinessRuleException("fromDate must be on or before toDate.");
        }
    }

    private static boolean isReceptionistWithoutElevatedRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        boolean receptionist = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_RECEPTIONIST"));
        boolean elevated = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_OWNER")
                        || authority.getAuthority().equals("PERMISSION_EXPENSES"));
        return receptionist && !elevated;
    }
}
