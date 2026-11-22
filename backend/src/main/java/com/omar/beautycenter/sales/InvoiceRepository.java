package com.omar.beautycenter.sales;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends Repository<Invoice, Long> {

    Optional<Invoice> findById(Long id);

    Optional<Invoice> findByClientRequestId(String clientRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT invoice FROM Invoice invoice WHERE invoice.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Long id);

    <S extends Invoice> S saveAndFlush(S invoice);

    @Query("""
            SELECT invoice
            FROM Invoice invoice
            WHERE (:status IS NULL OR invoice.status = :status)
              AND (:createdBy IS NULL OR invoice.createdBy = :createdBy)
              AND (CAST(:fromDate AS date) IS NULL OR invoice.businessDate >= :fromDate)
              AND (CAST(:toDate AS date) IS NULL OR invoice.businessDate <= :toDate)
              AND (:search IS NULL
                   OR LOWER(invoice.invoiceNumber) LIKE :search
                   OR LOWER(COALESCE(invoice.customerName, '')) LIKE :search
                   OR LOWER(COALESCE(invoice.customerPhone, '')) LIKE :search)
            """)
    Page<Invoice> search(
            @Param("status") InvoiceStatus status,
            @Param("createdBy") String createdBy,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("search") String search,
            Pageable pageable);
}
