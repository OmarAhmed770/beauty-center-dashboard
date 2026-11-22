package com.omar.beautycenter.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface HairPriceOverrideAuditRepository extends Repository<HairPriceOverrideAudit, Long> {

    <S extends HairPriceOverrideAudit> S save(S audit);

    @Query("""
            SELECT audit
            FROM HairPriceOverrideAudit audit
            WHERE (:configurationId IS NULL OR audit.hairConfiguration.id = :configurationId)
            """)
    Page<HairPriceOverrideAudit> search(
            @Param("configurationId") Long configurationId,
            Pageable pageable);
}
