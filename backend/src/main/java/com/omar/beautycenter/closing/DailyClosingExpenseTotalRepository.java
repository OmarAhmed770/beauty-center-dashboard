package com.omar.beautycenter.closing;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface DailyClosingExpenseTotalRepository
        extends Repository<DailyClosingExpenseTotal, Long> {

    List<DailyClosingExpenseTotal> findByClosingIdOrderByAmountDesc(Long closingId);

    long deleteByClosingId(Long closingId);

    <S extends DailyClosingExpenseTotal> List<S> saveAllAndFlush(Iterable<S> totals);
}
