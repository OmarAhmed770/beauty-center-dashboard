package com.omar.beautycenter.closing;

import com.omar.beautycenter.common.error.BusinessRuleException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessDateWriteGuardConfiguration {

    @Bean
    BusinessDateWriteGuard businessDateWriteGuard(
            DailyClosingRepository repository,
            BusinessDateTransactionLock transactionLock) {
        return businessDate -> {
            transactionLock.acquire(businessDate);
            repository.findTopByBusinessDateOrderByClosingVersionDesc(businessDate)
                    .map(DailyClosing::getStatus)
                    .filter(status -> status == DailyClosingStatus.CLOSED)
                    .ifPresent(status -> {
                        throw new BusinessRuleException(
                                "Financial records cannot be changed for a closed business date.");
                    });
        };
    }
}
