package com.omar.beautycenter.common.config;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {

    @Bean
    public Clock applicationClock(ApplicationProperties properties) {
        try {
            ZoneId zoneId = ZoneId.of(properties.timeZone());
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
            return Clock.system(zoneId);
        } catch (DateTimeException exception) {
            throw new IllegalStateException(
                    "Invalid app.time-zone value: " + properties.timeZone(), exception);
        }
    }
}
