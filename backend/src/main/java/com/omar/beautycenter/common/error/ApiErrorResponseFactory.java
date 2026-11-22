package com.omar.beautycenter.common.error;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ApiErrorResponseFactory {

    private final Clock clock;

    public ApiErrorResponseFactory(Clock clock) {
        this.clock = clock;
    }

    public ApiErrorResponse create(
            HttpStatus status,
            String code,
            String message,
            String path) {
        return create(status, code, message, path, List.of());
    }

    public ApiErrorResponse create(
            HttpStatus status,
            String code,
            String message,
            String path,
            List<ValidationErrorResponse> validationErrors) {
        return new ApiErrorResponse(
                LocalDateTime.now(clock),
                status.value(),
                code,
                message,
                path,
                List.copyOf(validationErrors));
    }
}
