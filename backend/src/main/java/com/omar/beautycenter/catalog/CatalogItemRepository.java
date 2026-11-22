package com.omar.beautycenter.catalog;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {

    boolean existsByCode(String code);

    Optional<CatalogItem> findByCode(String code);

    @Query("""
            SELECT item
            FROM CatalogItem item
            WHERE (:categoryId IS NULL OR item.category.id = :categoryId)
              AND (:itemType IS NULL OR item.itemType = :itemType)
              AND (:active IS NULL OR item.active = :active)
              AND (:search IS NULL
                   OR LOWER(item.code) LIKE :search
                   OR LOWER(item.nameArabic) LIKE :search
                   OR LOWER(item.nameEnglish) LIKE :search
                   OR LOWER(COALESCE(item.description, '')) LIKE :search
                   OR LOWER(item.category.nameArabic) LIKE :search
                   OR LOWER(item.category.nameEnglish) LIKE :search)
            """)
    Page<CatalogItem> search(
            @Param("categoryId") Long categoryId,
            @Param("itemType") CatalogItemType itemType,
            @Param("active") Boolean active,
            @Param("search") String search,
            Pageable pageable);
}
