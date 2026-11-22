package com.omar.beautycenter.employee;

import com.omar.beautycenter.common.dto.PageResponse;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.common.error.NotFoundException;
import com.omar.beautycenter.employee.dto.CreateEmployeeRequest;
import com.omar.beautycenter.employee.dto.EmployeeResponse;
import com.omar.beautycenter.employee.dto.UpdateEmployeeRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('EMPLOYEES')")
    public EmployeeResponse create(CreateEmployeeRequest request) {
        Employee employee = new Employee(
                requiredText(request.name(), "Name", 150),
                optionalText(request.phone(), "Phone", 50),
                requiredText(request.position(), "Position", 100),
                nonNegativeMoney(request.defaultDailyWage(), "Default daily wage"),
                optionalText(request.notes(), "Notes", 2000));
        return EmployeeResponse.from(employeeRepository.saveAndFlush(employee));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('EMPLOYEES')")
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        Employee employee = getEntityById(id);
        employee.update(
                requiredText(request.name(), "Name", 150),
                optionalText(request.phone(), "Phone", 50),
                requiredText(request.position(), "Position", 100),
                nonNegativeMoney(request.defaultDailyWage(), "Default daily wage"),
                optionalText(request.notes(), "Notes", 2000));
        return EmployeeResponse.from(employeeRepository.saveAndFlush(employee));
    }

    @Transactional
    @PreAuthorize("@permissionAccess.has('EMPLOYEES')")
    public EmployeeResponse setActive(Long id, boolean active) {
        Employee employee = getEntityById(id);
        employee.setActive(active);
        return EmployeeResponse.from(employeeRepository.saveAndFlush(employee));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('EMPLOYEES','ADD_ORDERS')")
    public EmployeeResponse getById(Long id) {
        return EmployeeResponse.from(getEntityById(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@permissionAccess.hasAny('EMPLOYEES','ADD_ORDERS')")
    public PageResponse<EmployeeResponse> search(
            String query,
            Boolean active,
            int page,
            int size) {
        String search = query == null || query.isBlank()
                ? null
                : "%" + query.strip().toLowerCase(Locale.ROOT) + "%";
        Page<EmployeeResponse> results = employeeRepository.search(
                        search,
                        active,
                        PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id"))))
                .map(EmployeeResponse::from);
        return PageResponse.from(results);
    }

    @Transactional(readOnly = true)
    public Employee getEntityById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found."));
    }

    static String requiredText(String value, String field, int maximumLength) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new BusinessRuleException(
                    field + " must contain 1 to " + maximumLength + " characters.");
        }
        return normalized;
    }

    static String optionalText(String value, String field, int maximumLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maximumLength) {
            throw new BusinessRuleException(
                    field + " must not exceed " + maximumLength + " characters.");
        }
        return normalized;
    }

    static BigDecimal nonNegativeMoney(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new BusinessRuleException(field + " must not be negative.");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BusinessRuleException(field + " must have at most 2 decimal digits.");
        }
    }
}
