package com.omar.beautycenter.expense;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    Optional<ExpenseCategory> findByNameEnglishIgnoreCase(String nameEnglish);

    boolean existsByNameArabicIgnoreCase(String nameArabic);

    boolean existsByNameEnglishIgnoreCase(String nameEnglish);

    boolean existsByNameArabicIgnoreCaseAndIdNot(String nameArabic, Long id);

    boolean existsByNameEnglishIgnoreCaseAndIdNot(String nameEnglish, Long id);

    @Query("""
            SELECT category
            FROM ExpenseCategory category
            WHERE (:active IS NULL OR category.active = :active)
              AND (:search IS NULL
                   OR LOWER(category.nameArabic) LIKE :search
                   OR LOWER(category.nameEnglish) LIKE :search)
            """)
    Page<ExpenseCategory> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable);
}
