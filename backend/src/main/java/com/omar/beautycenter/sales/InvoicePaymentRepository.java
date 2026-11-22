package com.omar.beautycenter.sales;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface InvoicePaymentRepository extends Repository<InvoicePayment, Long> {

    List<InvoicePayment> findAllByInvoiceIdOrderById(Long invoiceId);

    <S extends InvoicePayment> List<S> saveAll(Iterable<S> payments);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM InvoicePayment payment WHERE payment.invoice.id = :invoiceId")
    void deleteAllByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("SELECT COALESCE(SUM(payment.amount), 0) FROM InvoicePayment payment WHERE payment.invoice.id = :invoiceId")
    BigDecimal sumByInvoiceId(@Param("invoiceId") Long invoiceId);
}
