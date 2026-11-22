package com.omar.beautycenter.sales;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface InvoiceRefundRepository extends Repository<InvoiceRefund, Long> {

    <S extends InvoiceRefund> S saveAndFlush(S refund);

    List<InvoiceRefund> findAllByInvoiceIdOrderById(Long invoiceId);
}
