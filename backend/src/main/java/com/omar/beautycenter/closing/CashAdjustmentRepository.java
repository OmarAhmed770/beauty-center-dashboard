package com.omar.beautycenter.closing;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CashAdjustmentRepository extends Repository<CashAdjustment, Long> {

    Optional<CashAdjustment> findById(Long id);

    <S extends CashAdjustment> S saveAndFlush(S adjustment);

    @Query("""
            SELECT adjustment FROM CashAdjustment adjustment
            WHERE (:type IS NULL OR adjustment.type = :type)
              AND (:status IS NULL OR adjustment.status = :status)
              AND (:fromDate IS NULL OR adjustment.businessDate >= :fromDate)
              AND (:toDate IS NULL OR adjustment.businessDate <= :toDate)
            """)
    Page<CashAdjustment> search(
            @Param("type") CashAdjustmentType type,
            @Param("status") CashAdjustmentStatus status,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);
}
