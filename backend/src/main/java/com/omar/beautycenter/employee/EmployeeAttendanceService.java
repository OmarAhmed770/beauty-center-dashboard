package com.omar.beautycenter.employee;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.closing.BusinessDateWriteGuard;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.employee.dto.EmployeeAttendanceResponse;
import com.omar.beautycenter.employee.dto.SaveEmployeeAttendanceRequest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeAttendanceService {

    private final EmployeeAttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;
    private final EmployeeTransactionRepository transactionRepository;
    private final BusinessDateWriteGuard businessDateWriteGuard;
    private final AuditorAware<String> auditorAware;
    private final Clock clock;
    private final AuditLogService auditLogService;

    public EmployeeAttendanceService(
            EmployeeAttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository,
            EmployeeService employeeService,
            EmployeeTransactionRepository transactionRepository,
            BusinessDateWriteGuard businessDateWriteGuard,
            AuditorAware<String> auditorAware,
            Clock clock,
            AuditLogService auditLogService) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.employeeService = employeeService;
        this.transactionRepository = transactionRepository;
        this.businessDateWriteGuard = businessDateWriteGuard;
        this.auditorAware = auditorAware;
        this.clock = clock;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('OWNER')")
    public List<EmployeeAttendanceResponse> list(LocalDate businessDate) {
        requireBusinessDate(businessDate);
        Map<Long, EmployeeAttendanceResponse> rows = new LinkedHashMap<>();
        for (Employee employee : employeeRepository.findByActiveTrueOrderByNameAscIdAsc()) {
            rows.put(employee.getId(), EmployeeAttendanceResponse.notRecorded(employee, businessDate));
        }
        for (EmployeeAttendance attendance : attendanceRepository.findWithDetailsByBusinessDate(businessDate)) {
            rows.put(attendance.getEmployee().getId(), EmployeeAttendanceResponse.from(attendance));
        }
        List<EmployeeAttendanceResponse> result = new ArrayList<>(rows.values());
        result.sort(Comparator.comparing(EmployeeAttendanceResponse::employeeName)
                .thenComparing(EmployeeAttendanceResponse::employeeId));
        return result;
    }

    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public EmployeeAttendanceResponse save(
            Long employeeId,
            LocalDate businessDate,
            SaveEmployeeAttendanceRequest request) {
        requireBusinessDate(businessDate);
        if (request.status() == null) {
            throw new BusinessRuleException("Attendance status is required.");
        }
        businessDateWriteGuard.assertWritable(businessDate);

        EmployeeAttendance attendance = attendanceRepository.findByEmployeeIdAndBusinessDate(employeeId, businessDate)
                .orElseGet(() -> newAttendance(employeeId, businessDate, request.status()));
        if (attendance.getStatus() == request.status()
                && (request.status() == EmployeeAttendanceStatus.ABSENT
                || hasActiveLinkedWage(attendance))) {
            return EmployeeAttendanceResponse.from(attendance);
        }

        if (request.status() == EmployeeAttendanceStatus.PRESENT) {
            EmployeeTransaction wage = resolveDailyWage(attendance);
            attendance.markPresent(wage);
        } else {
            reverseUnpaidDailyWage(attendance);
            attendance.markAbsent();
        }

        EmployeeAttendance saved = attendanceRepository.saveAndFlush(attendance);
        recordAttendanceAudit(saved);
        return EmployeeAttendanceResponse.from(saved);
    }

    private EmployeeAttendance newAttendance(
            Long employeeId,
            LocalDate businessDate,
            EmployeeAttendanceStatus status) {
        Employee employee = employeeService.getEntityById(employeeId);
        if (!employee.isActive()) {
            throw new BusinessRuleException("Attendance can only be recorded for an active employee.");
        }
        return new EmployeeAttendance(employee, businessDate, status, employee.getDefaultDailyWage());
    }

    private EmployeeTransaction resolveDailyWage(EmployeeAttendance attendance) {
        if (hasActiveLinkedWage(attendance)) {
            return attendance.getDailyWageTransaction();
        }

        List<EmployeeTransaction> existingWages = transactionRepository.findActiveDailyWages(
                attendance.getEmployee().getId(), attendance.getBusinessDate());
        if (existingWages.size() > 1) {
            throw new BusinessRuleException(
                    "More than one active daily wage exists for this employee and date. Correct the existing financial entries first.");
        }
        if (existingWages.size() == 1) {
            return existingWages.get(0);
        }
        if (attendance.getDailyWageAmount().signum() <= 0) {
            throw new BusinessRuleException(
                    "Set a positive daily wage for the employee before marking attendance present.");
        }

        EmployeeTransaction wage = new EmployeeTransaction(
                attendance.getEmployee(),
                EmployeeTransactionType.DAILY_WAGE,
                attendance.getDailyWageAmount(),
                attendance.getBusinessDate(),
                null,
                "Attendance-generated daily wage");
        EmployeeTransaction saved = transactionRepository.saveAndFlush(wage);
        auditLogService.record(
                "EMPLOYEE_DAILY_WAGE_ACCRUED",
                "EMPLOYEE_TRANSACTION",
                saved.getId(),
                saved.getBusinessDate(),
                Map.of("employeeId", saved.getEmployee().getId(), "amount", saved.getAmount()));
        return saved;
    }

    private void reverseUnpaidDailyWage(EmployeeAttendance attendance) {
        EmployeeTransaction wage = attendance.getDailyWageTransaction();
        if (wage == null || wage.getStatus() == EmployeeTransactionStatus.CANCELLED) {
            return;
        }
        if (wage.getPaymentMethodType() != null
                || transactionRepository.countActivePaymentsOnOrAfter(
                        attendance.getEmployee().getId(), attendance.getBusinessDate()) > 0) {
            throw new BusinessRuleException(
                    "This attendance wage may already be settled. Correct the employee payment before marking attendance absent.");
        }

        String actor = auditorAware.getCurrentAuditor().orElse("SYSTEM");
        wage.cancel(LocalDateTime.now(clock), actor, "Attendance corrected to absent.");
        transactionRepository.saveAndFlush(wage);
        auditLogService.record(
                "EMPLOYEE_ATTENDANCE_WAGE_REVERSED",
                "EMPLOYEE_TRANSACTION",
                wage.getId(),
                wage.getBusinessDate(),
                Map.of("employeeId", wage.getEmployee().getId(), "amount", wage.getAmount()));
    }

    private static boolean hasActiveLinkedWage(EmployeeAttendance attendance) {
        return attendance.getDailyWageTransaction() != null
                && attendance.getDailyWageTransaction().getStatus() == EmployeeTransactionStatus.ACTIVE;
    }

    private void recordAttendanceAudit(EmployeeAttendance attendance) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("employeeId", attendance.getEmployee().getId());
        details.put("status", attendance.getStatus());
        details.put("dailyWageAmount", attendance.getDailyWageAmount());
        if (attendance.getDailyWageTransaction() != null) {
            details.put("dailyWageTransactionId", attendance.getDailyWageTransaction().getId());
        }
        auditLogService.record(
                attendance.getStatus() == EmployeeAttendanceStatus.PRESENT
                        ? "EMPLOYEE_ATTENDANCE_MARKED_PRESENT"
                        : "EMPLOYEE_ATTENDANCE_MARKED_ABSENT",
                "EMPLOYEE_ATTENDANCE",
                attendance.getId(),
                attendance.getBusinessDate(),
                details);
    }

    private static void requireBusinessDate(LocalDate businessDate) {
        if (businessDate == null) {
            throw new BusinessRuleException("Business date is required.");
        }
    }
}
