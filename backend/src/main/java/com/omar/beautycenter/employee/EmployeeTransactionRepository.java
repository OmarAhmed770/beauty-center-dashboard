package com.omar.beautycenter.employee;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface EmployeeTransactionRepository
        extends Repository<EmployeeTransaction, Long>, JpaSpecificationExecutor<EmployeeTransaction> {

    Optional<EmployeeTransaction> findById(Long id);

    <S extends EmployeeTransaction> S saveAndFlush(S transaction);

    default Page<EmployeeTransaction> search(
            Long employeeId,
            EmployeeTransactionType type,
            EmployeeTransactionStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        // Omit absent filters instead of binding untyped PostgreSQL "? IS NULL" parameters.
        return findAll((root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (employeeId != null) predicates.add(builder.equal(root.get("employee").get("id"), employeeId));
            if (type != null) predicates.add(builder.equal(root.get("type"), type));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (fromDate != null) predicates.add(builder.greaterThanOrEqualTo(root.<LocalDate>get("businessDate"), fromDate));
            if (toDate != null) predicates.add(builder.lessThanOrEqualTo(root.<LocalDate>get("businessDate"), toDate));
            return builder.and(predicates.toArray(Predicate[]::new));
        }, pageable);
    }

    @Query("""
            SELECT transaction.type AS type, SUM(transaction.amount) AS total
            FROM EmployeeTransaction transaction
            WHERE transaction.employee.id = :employeeId
              AND transaction.status = com.omar.beautycenter.employee.EmployeeTransactionStatus.ACTIVE
              AND transaction.businessDate BETWEEN :fromDate AND :toDate
            GROUP BY transaction.type
            """)
    List<EmployeeTransactionTotal> sumActiveByType(
            @Param("employeeId") Long employeeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT COALESCE(SUM(transaction.amount), 0)
            FROM EmployeeTransaction transaction
            WHERE transaction.employee.id = :employeeId
              AND transaction.status = com.omar.beautycenter.employee.EmployeeTransactionStatus.ACTIVE
              AND transaction.businessDate BETWEEN :fromDate AND :toDate
              AND transaction.type IN (
                  com.omar.beautycenter.employee.EmployeeTransactionType.DAILY_WAGE,
                  com.omar.beautycenter.employee.EmployeeTransactionType.BONUS,
                  com.omar.beautycenter.employee.EmployeeTransactionType.COMMISSION)
              AND transaction.paymentMethodType IS NOT NULL
            """)
    BigDecimal sumActiveImmediatelyPaidEntitlements(
            @Param("employeeId") Long employeeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT transaction
            FROM EmployeeTransaction transaction
            WHERE transaction.employee.id = :employeeId
              AND transaction.businessDate = :businessDate
              AND transaction.type = com.omar.beautycenter.employee.EmployeeTransactionType.DAILY_WAGE
              AND transaction.status = com.omar.beautycenter.employee.EmployeeTransactionStatus.ACTIVE
            ORDER BY transaction.id
            """)
    List<EmployeeTransaction> findActiveDailyWages(
            @Param("employeeId") Long employeeId,
            @Param("businessDate") LocalDate businessDate);

    @Query("""
            SELECT COUNT(transaction)
            FROM EmployeeTransaction transaction
            WHERE transaction.employee.id = :employeeId
              AND transaction.type = com.omar.beautycenter.employee.EmployeeTransactionType.PAYMENT
              AND transaction.status = com.omar.beautycenter.employee.EmployeeTransactionStatus.ACTIVE
              AND transaction.businessDate >= :businessDate
            """)
    long countActivePaymentsOnOrAfter(
            @Param("employeeId") Long employeeId,
            @Param("businessDate") LocalDate businessDate);
}
