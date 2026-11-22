package com.omar.beautycenter.employee;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.closing.BusinessDateWriteGuard;
import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import com.omar.beautycenter.employee.dto.CancelEmployeeTransactionRequest;
import com.omar.beautycenter.employee.dto.CreateEmployeeTransactionRequest;
import com.omar.beautycenter.employee.dto.EmployeeResponse;
import com.omar.beautycenter.employee.dto.EmployeeStatementResponse;
import com.omar.beautycenter.employee.dto.EmployeeTotalsResponse;
import com.omar.beautycenter.employee.dto.EmployeeTransactionResponse;
import com.omar.beautycenter.payment.PaymentMethod;
import com.omar.beautycenter.payment.PaymentMethodService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeTransactionService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final EmployeeTransactionRepository transactionRepository;
    private final EmployeeService employeeService;
    private final PaymentMethodService paymentMethodService;
    private final BusinessDateWriteGuard businessDateWriteGuard;
    private final AuditorAware<String> auditorAware;
    private final Clock clock;
    private final AuditLogService auditLogService;
    private final EmployeeAttendanceRepository attendanceRepository;

    public EmployeeTransactionService(
            EmployeeTransactionRepository transactionRepository,
            EmployeeService employeeService,
            PaymentMethodService paymentMethodService,
            BusinessDateWriteGuard businessDateWriteGuard,
            AuditorAware<String> auditorAware,
            Clock clock,
            AuditLogService auditLogService,
            EmployeeAttendanceRepository attendanceRepository) {
        this.transactionRepository = transactionRepository;
        this.employeeService = employeeService;
        this.paymentMethodService = paymentMethodService;
        this.businessDateWriteGuard = businessDateWriteGuard;
        this.auditorAware = auditorAware;
        this.clock = clock;
        this.auditLogService = auditLogService;
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('EMPLOYEES')")
    public EmployeeTransactionResponse create(CreateEmployeeTransactionRequest request) {
        if (request.type() == null) {
            throw new BusinessRuleException("Transaction type is required.");
        }
        Employee employee = employeeService.getEntityById(request.employeeId());
        if (!employee.isActive()) {
            throw new BusinessRuleException(
                    "Transactions can only be created for an active employee.");
        }

        if (request.businessDate() == null) {
            throw new BusinessRuleException("Business date is required.");
        }
        businessDateWriteGuard.assertWritable(request.businessDate());
        if (request.type() == EmployeeTransactionType.DAILY_WAGE
                && attendanceRepository.existsByEmployeeIdAndBusinessDateAndStatus(
                        employee.getId(), request.businessDate(), EmployeeAttendanceStatus.PRESENT)) {
            throw new BusinessRuleException(
                    "Daily wage is already managed by the employee attendance record for this date.");
        }

        PaymentMethod paymentMethod =
                paymentMethodService.getActiveEntityByCode(request.paymentMethod());
        EmployeeTransaction transaction = new EmployeeTransaction(
                employee,
                request.type(),
                positiveMoney(request.amount()),
                request.businessDate(),
                paymentMethod,
                EmployeeService.optionalText(request.notes(), "Notes", 2000));
        return EmployeeTransactionResponse.from(transactionRepository.saveAndFlush(transaction));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('EMPLOYEES')")
    public EmployeeTransactionResponse cancel(
            Long transactionId,
            CancelEmployeeTransactionRequest request) {
        EmployeeTransaction transaction = getEntityById(transactionId);
        if (transaction.getStatus() == EmployeeTransactionStatus.CANCELLED) {
            throw new BusinessRuleException("The employee transaction is already cancelled.");
        }

        businessDateWriteGuard.assertWritable(transaction.getBusinessDate());
        String reason = EmployeeService.requiredText(request.reason(), "Cancellation reason", 500);
        String actor = auditorAware.getCurrentAuditor().orElse("SYSTEM");
        transaction.cancel(LocalDateTime.now(clock), actor, reason);
        transactionRepository.saveAndFlush(transaction);
        auditLogService.record("EMPLOYEE_TRANSACTION_CANCELLED", "EMPLOYEE_TRANSACTION",
                transaction.getId(), transaction.getBusinessDate(), Map.of(
                        "employeeId", transaction.getEmployee().getId(), "type", transaction.getType(),
                        "amount", transaction.getAmount(), "reason", reason));
        return EmployeeTransactionResponse.from(transaction);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('EMPLOYEES')")
    public EmployeeTransactionResponse getById(Long transactionId) {
        return EmployeeTransactionResponse.from(getEntityById(transactionId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('EMPLOYEES')")
    public PageResponse<EmployeeTransactionResponse> search(
            Long employeeId,
            EmployeeTransactionType type,
            EmployeeTransactionStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {
        validateDateRange(fromDate, toDate, false);
        Page<EmployeeTransactionResponse> results = transactionRepository.search(
                        employeeId,
                        type,
                        status,
                        fromDate,
                        toDate,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Order.desc("businessDate"), Sort.Order.desc("id"))))
                .map(EmployeeTransactionResponse::from);
        return PageResponse.from(results);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('EMPLOYEES')")
    public EmployeeTotalsResponse totals(Long employeeId, LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate, true);
        Employee employee = employeeService.getEntityById(employeeId);
        return calculateTotals(employee, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.has('EMPLOYEES')")
    public EmployeeStatementResponse statement(
            Long employeeId,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {
        validateDateRange(fromDate, toDate, true);
        Employee employee = employeeService.getEntityById(employeeId);
        EmployeeTotalsResponse totals = calculateTotals(employee, fromDate, toDate);
        Page<EmployeeTransactionResponse> transactions = transactionRepository.search(
                        employeeId,
                        null,
                        null,
                        fromDate,
                        toDate,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Order.asc("businessDate"), Sort.Order.asc("id"))))
                .map(EmployeeTransactionResponse::from);
        return new EmployeeStatementResponse(
                EmployeeResponse.from(employee),
                fromDate,
                toDate,
                totals,
                PageResponse.from(transactions));
    }

    private EmployeeTotalsResponse calculateTotals(
            Employee employee,
            LocalDate fromDate,
            LocalDate toDate) {
        Map<EmployeeTransactionType, BigDecimal> values =
                new EnumMap<>(EmployeeTransactionType.class);
        for (EmployeeTransactionTotal total : transactionRepository.sumActiveByType(
                employee.getId(), fromDate, toDate)) {
            values.put(total.getType(), money(total.getTotal()));
        }

        BigDecimal dailyWages = values.getOrDefault(EmployeeTransactionType.DAILY_WAGE, ZERO);
        BigDecimal bonuses = values.getOrDefault(EmployeeTransactionType.BONUS, ZERO);
        BigDecimal commissions = values.getOrDefault(EmployeeTransactionType.COMMISSION, ZERO);
        BigDecimal deductions = values.getOrDefault(EmployeeTransactionType.DEDUCTION, ZERO);
        BigDecimal advances = values.getOrDefault(EmployeeTransactionType.ADVANCE, ZERO);
        BigDecimal payments = values.getOrDefault(EmployeeTransactionType.PAYMENT, ZERO);
        BigDecimal immediatelyPaidEntitlements = money(transactionRepository
                .sumActiveImmediatelyPaidEntitlements(employee.getId(), fromDate, toDate));

        BigDecimal netEntitlement = dailyWages
                .add(bonuses)
                .add(commissions)
                .subtract(deductions)
                .subtract(advances)
                .subtract(payments)
                .subtract(immediatelyPaidEntitlements)
                .setScale(2, RoundingMode.UNNECESSARY);

        return new EmployeeTotalsResponse(
                employee.getId(),
                employee.getName(),
                fromDate,
                toDate,
                dailyWages,
                bonuses,
                commissions,
                deductions,
                advances,
                payments,
                netEntitlement);
    }

    private EmployeeTransaction getEntityById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Employee transaction not found."));
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
}
