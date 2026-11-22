package com.omar.beautycenter.sales;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface InvoiceItemRepository extends Repository<InvoiceItem, Long> {

    Optional<InvoiceItem> findById(Long id);

    List<InvoiceItem> findAllByInvoiceIdOrderById(Long invoiceId);

    long countByInvoiceId(Long invoiceId);

    <S extends InvoiceItem> S saveAndFlush(S item);

    void delete(InvoiceItem item);
}
