package com.omar.beautycenter.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends Repository<Expense, Long> {

    Optional<Expense> findById(Long id);

    <S extends Expense> S saveAndFlush(S expense);

    @Query("""
            SELECT entry
            FROM Expense entry
            WHERE (:categoryId IS NULL OR entry.category.id = :categoryId)
              AND (:paymentMethodId IS NULL OR entry.paymentMethod.id = :paymentMethodId)
              AND (:status IS NULL OR entry.status = :status)
              AND (CAST(:fromDate AS date) IS NULL OR entry.businessDate >= :fromDate)
              AND (CAST(:toDate AS date) IS NULL OR entry.businessDate <= :toDate)
              AND (:search IS NULL
                   OR LOWER(entry.description) LIKE :search
                   OR LOWER(COALESCE(entry.paymentReference, '')) LIKE :search
                   OR LOWER(COALESCE(entry.receiptPath, '')) LIKE :search
                   OR LOWER(entry.categoryNameArabic) LIKE :search
                   OR LOWER(entry.categoryNameEnglish) LIKE :search
                   OR LOWER(entry.paymentMethodCode) LIKE :search
                   OR LOWER(entry.paymentMethodNameArabic) LIKE :search
                   OR LOWER(entry.paymentMethodNameEnglish) LIKE :search)
            """)
    Page<Expense> search(
            @Param("categoryId") Long categoryId,
            @Param("paymentMethodId") Long paymentMethodId,
            @Param("status") ExpenseStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(entry.amount), 0)
            FROM Expense entry
            WHERE entry.status = com.omar.beautycenter.expense.ExpenseStatus.ACTIVE
              AND entry.businessDate BETWEEN :fromDate AND :toDate
            """)
    BigDecimal sumActiveAmount(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT COALESCE(SUM(entry.amount), 0)
            FROM Expense entry
            WHERE entry.status = com.omar.beautycenter.expense.ExpenseStatus.ACTIVE
              AND entry.paymentMethodType = com.omar.beautycenter.payment.PaymentMethodType.CASH
              AND entry.businessDate BETWEEN :fromDate AND :toDate
            """)
    BigDecimal sumActiveCashAmount(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT COUNT(entry)
            FROM Expense entry
            WHERE entry.status = :status
              AND entry.businessDate BETWEEN :fromDate AND :toDate
            """)
    long countByStatusAndDateRange(
            @Param("status") ExpenseStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT entry.category.id AS categoryId,
                   entry.categoryNameArabic AS nameArabic,
                   entry.categoryNameEnglish AS nameEnglish,
                   SUM(entry.amount) AS totalAmount,
                   COUNT(entry) AS expenseCount
            FROM Expense entry
            WHERE entry.status = com.omar.beautycenter.expense.ExpenseStatus.ACTIVE
              AND entry.businessDate BETWEEN :fromDate AND :toDate
            GROUP BY entry.category.id, entry.categoryNameArabic, entry.categoryNameEnglish
            ORDER BY SUM(entry.amount) DESC, entry.categoryNameEnglish ASC
            """)
    List<ExpenseCategoryTotal> groupActiveByCategory(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT entry.paymentMethod.id AS paymentMethodId,
                   entry.paymentMethodCode AS code,
                   entry.paymentMethodNameArabic AS displayNameArabic,
                   entry.paymentMethodNameEnglish AS displayNameEnglish,
                   entry.paymentMethodType AS type,
                   SUM(entry.amount) AS totalAmount,
                   COUNT(entry) AS expenseCount
            FROM Expense entry
            WHERE entry.status = com.omar.beautycenter.expense.ExpenseStatus.ACTIVE
              AND entry.businessDate BETWEEN :fromDate AND :toDate
            GROUP BY entry.paymentMethod.id, entry.paymentMethodCode,
                     entry.paymentMethodNameArabic,
                     entry.paymentMethodNameEnglish,
                     entry.paymentMethodType,
                     entry.paymentMethod.sortOrder
            ORDER BY entry.paymentMethod.sortOrder ASC, entry.paymentMethodCode ASC
            """)
    List<ExpensePaymentMethodTotal> groupActiveByPaymentMethod(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
