package com.omar.beautycenter.catalog;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HairStockMovementRepository extends JpaRepository<HairStockMovement, Long> {

    Page<HairStockMovement> findAllByHairItemId(Long hairItemId, Pageable pageable);

    boolean existsByEventKey(String eventKey);

    @Query("""
            SELECT COALESCE(SUM(movement.adjustmentGrams), 0)
            FROM HairStockMovement movement
            WHERE movement.invoiceItem.id = :invoiceItemId
              AND movement.movementType = :movementType
            """)
    BigDecimal sumAdjustmentsByInvoiceItemAndType(
            @Param("invoiceItemId") Long invoiceItemId,
            @Param("movementType") HairStockMovementType movementType);
}
