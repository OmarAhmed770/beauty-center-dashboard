package com.omar.beautycenter.sales;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface InvoiceRefundPaymentRepository extends Repository<InvoiceRefundPayment, Long> {

    <S extends InvoiceRefundPayment> List<S> saveAll(Iterable<S> payments);

    List<InvoiceRefundPayment> findAllByRefundIdOrderById(Long refundId);
}
