package com.omar.beautycenter.closing;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface DailyClosingPaymentTotalRepository
        extends Repository<DailyClosingPaymentTotal, Long> {

    List<DailyClosingPaymentTotal> findByClosingIdOrderByCode(Long closingId);

    long deleteByClosingId(Long closingId);

    <S extends DailyClosingPaymentTotal> List<S> saveAllAndFlush(Iterable<S> totals);
}
