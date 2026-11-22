package com.omar.beautycenter.catalog;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, Long> {

    boolean existsByNameArabicIgnoreCase(String nameArabic);

    boolean existsByNameEnglishIgnoreCase(String nameEnglish);

    boolean existsByNameArabicIgnoreCaseAndIdNot(String nameArabic, Long id);

    boolean existsByNameEnglishIgnoreCaseAndIdNot(String nameEnglish, Long id);

    Optional<CatalogCategory> findByNameEnglishIgnoreCase(String nameEnglish);

    Optional<CatalogCategory> findByNameArabicIgnoreCase(String nameArabic);

    @Query("""
            SELECT category
            FROM CatalogCategory category
            WHERE (:active IS NULL OR category.active = :active)
              AND (:search IS NULL
                   OR LOWER(category.nameArabic) LIKE :search
                   OR LOWER(category.nameEnglish) LIKE :search
                   OR LOWER(COALESCE(category.description, '')) LIKE :search)
            """)
    Page<CatalogCategory> search(
            @Param("search") String search,
            @Param("active") Boolean active,
            Pageable pageable);
}
